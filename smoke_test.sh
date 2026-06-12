#!/bin/bash

BASE_URL="http://localhost:8080/api"
PASSED=0
FAILED=0

echo "========================================="
echo "  诊所复诊随访系统 - 冒烟测试"
echo "========================================="
echo ""

wait_for_service() {
    echo "等待服务启动..."
    for i in {1..30}; do
        if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/plans" | grep -q "200\|404"; then
            echo "服务已启动！"
            return 0
        fi
        echo -n "."
        sleep 2
    done
    echo ""
    echo "错误：服务启动超时"
    exit 1
}

test_case() {
    local test_name=$1
    local expected_status=$2
    local actual_status=$3
    local description=$4

    if [ "$actual_status" = "$expected_status" ]; then
        echo "✅ PASS: $test_name"
        echo "   $description"
        PASSED=$((PASSED + 1))
    else
        echo "❌ FAIL: $test_name"
        echo "   期望状态: $expected_status, 实际状态: $actual_status"
        echo "   $description"
        FAILED=$((FAILED + 1))
    fi
    echo ""
}

extract_json() {
    echo "$1" | python3 -c "$2" 2>/dev/null || echo ""
}

wait_for_service

# ============================================
# 测试 1: 转院患者不生成随访节点
# ============================================
echo ""
echo "-----------------------------------------"
echo "测试 1: 转院患者不生成随访节点"
echo "-----------------------------------------"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/plans" \
    -H "Content-Type: application/json" \
    -d '{
        "patientName": "转院患者",
        "patientIdCard": "110101199001011234",
        "patientPhone": "13800138000",
        "dischargeDate": "2025-01-15",
        "diseaseType": "高血压",
        "riskLevel": "HIGH",
        "transferStatus": "TRANSFERRED",
        "attendingDoctor": "张医生",
        "assignedNurse": "李护士"
    }')
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
NODES_COUNT=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',{}).get('nodes',[])))")

test_case "转院患者计划创建" "200" "$HTTP_STATUS" "创建转院患者的随访计划"
test_case "转院患者无随访节点" "0" "$NODES_COUNT" "已转院患者不应生成随访节点，实际节点数: $NODES_COUNT"

TRANSFER_PLAN_ID=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('id',''))")

# ============================================
# 测试 2: 正常患者生成随访节点
# ============================================
echo ""
echo "-----------------------------------------"
echo "测试 2: 正常患者生成随访节点"
echo "-----------------------------------------"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/plans" \
    -H "Content-Type: application/json" \
    -d '{
        "patientName": "正常患者",
        "patientIdCard": "110101199002025678",
        "patientPhone": "13900139000",
        "dischargeDate": "2025-01-15",
        "diseaseType": "糖尿病",
        "riskLevel": "MEDIUM",
        "transferStatus": "NOT_TRANSFERRED",
        "attendingDoctor": "王医生",
        "assignedNurse": "赵护士"
    }')
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
NODES_COUNT=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',{}).get('nodes',[])))")
RISK_INTERVAL=$(extract_json "$BODY" "
import sys,json
from datetime import datetime
d=json.load(sys.stdin)
nodes = d.get('data',{}).get('nodes',[])
if len(nodes) >= 2:
    d1 = datetime.strptime(nodes[0]['followUpDate'], '%Y-%m-%d')
    d2 = datetime.strptime(nodes[1]['followUpDate'], '%Y-%m-%d')
    print((d2 - d1).days)
else:
    print(0)
")

test_case "正常患者计划创建" "200" "$HTTP_STATUS" "创建未转院患者的随访计划"
test_case "正常患者生成4个随访节点" "4" "$NODES_COUNT" "未转院患者应生成4个随访节点，实际节点数: $NODES_COUNT"
test_case "中风险间隔7天" "7" "$RISK_INTERVAL" "中风险患者随访间隔应为7天，实际间隔: $RISK_INTERVAL 天"

NORMAL_PLAN_ID=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('id',''))")

# ============================================
# 测试 3: 复诊日期早于出院日期拦截
# ============================================
echo ""
echo "-----------------------------------------"
echo "测试 3: 复诊日期早于出院日期拦截"
echo "-----------------------------------------"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/records" \
    -H "Content-Type: application/json" \
    -d '{
        "planId": '"$NORMAL_PLAN_ID"',
        "callResult": "CONNECTED",
        "conversationContent": "患者状态良好",
        "needExamReport": true,
        "nextReminderDate": "2025-01-10",
        "operatorName": "李护士"
    }')
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
ERROR_MSG=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('message',''))")

test_case "日期错误被后端拦截" "400" "$HTTP_STATUS" "复诊日期(2025-01-10)早于出院日期(2025-01-15)应被拦截，错误信息: $ERROR_MSG"

# ============================================
# 测试 4: 连续未联系达到阈值自动升级 (回归验证)
# ============================================
echo ""
echo "-----------------------------------------"
echo "测试 4: 连续未联系达到阈值自动升级 (回归验证)"
echo "-----------------------------------------"

for i in 1 2; do
    RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/records" \
        -H "Content-Type: application/json" \
        -d '{
            "planId": '"$NORMAL_PLAN_ID"',
            "callResult": "NO_ANSWER",
            "noAnswerReason": "电话无人接听",
            "operatorName": "赵护士"
        }')
    HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
    echo "  第 $i 次未接通记录: HTTP $HTTP_STATUS"
done

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/plans/$NORMAL_PLAN_ID")
BODY=$(echo "$RESPONSE" | sed '$d')
PLAN_STATUS_AFTER_2=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('status',''))")
CONSECUTIVE_AFTER_2=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('consecutiveMissed',0))")

test_case "2次未接通后状态仍为ACTIVE" "ACTIVE" "$PLAN_STATUS_AFTER_2" "连续2次未接通不应升级，实际状态: $PLAN_STATUS_AFTER_2"
test_case "2次未接通计数为2" "2" "$CONSECUTIVE_AFTER_2" "连续未接通计数应为2，实际: $CONSECUTIVE_AFTER_2"

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/records" \
    -H "Content-Type: application/json" \
    -d '{
        "planId": '"$NORMAL_PLAN_ID"',
        "callResult": "NO_ANSWER",
        "noAnswerReason": "电话无人接听-第3次",
        "operatorName": "赵护士"
    }')
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
echo "  第 3 次未接通记录: HTTP $HTTP_STATUS"

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/plans/$NORMAL_PLAN_ID")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
PLAN_STATUS=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('status',''))")
CONSECUTIVE_MISSED=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('consecutiveMissed',0))")

test_case "连续3次未接通后计划状态变为ESCALATED" "ESCALATED" "$PLAN_STATUS" "连续3次未接通后计划应自动升级，实际状态: $PLAN_STATUS"
test_case "连续未接通计数为3" "3" "$CONSECUTIVE_MISSED" "连续未接通计数应为3，实际: $CONSECUTIVE_MISSED"

ESC_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/escalations/plan/$NORMAL_PLAN_ID/active")
ESC_BODY=$(echo "$ESC_RESPONSE" | sed '$d')
ESC_ID=$(extract_json "$ESC_BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('id',''))")
HAS_ACTIVE_ESC="NOT_EMPTY"
if [ -z "$ESC_ID" ] || [ "$ESC_ID" = "None" ]; then
    HAS_ACTIVE_ESC="EMPTY"
fi
test_case "升级后存在活跃升级记录" "NOT_EMPTY" "$HAS_ACTIVE_ESC" "自动升级应创建活跃升级记录，升级ID: $ESC_ID"

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/plans/escalated")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
ESCALATED_IN_LIST=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); plans=d.get('data',[]); print(len([p for p in plans if p.get('id')==$NORMAL_PLAN_ID]))")
test_case "升级列表包含该计划" "1" "$ESCALATED_IN_LIST" "升级列表应包含该升级状态的计划"

# ============================================
# 测试 5: 升级后护士不能直接关闭计划 (回归验证)
# ============================================
echo ""
echo "-----------------------------------------"
echo "测试 5: 升级后护士不能直接关闭计划 (回归验证)"
echo "-----------------------------------------"
REASON_ENCODED=$(python3 -c "import urllib.parse; print(urllib.parse.quote('测试关闭'))" 2>/dev/null || echo "test")
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/escalations/close-plan/$NORMAL_PLAN_ID?reason=$REASON_ENCODED" \
    -H "Content-Type: application/json")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
ERROR_MSG=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('message',''))")

test_case "升级后护士关闭被拦截" "400" "$HTTP_STATUS" "存在未处理升级记录时护士不能直接关闭计划，错误信息: $ERROR_MSG"

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/plans/$NORMAL_PLAN_ID")
BODY=$(echo "$RESPONSE" | sed '$d')
PLAN_STATUS_AFTER_ATTEMPT=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('status',''))")
test_case "拦截后计划仍为ESCALATED" "ESCALATED" "$PLAN_STATUS_AFTER_ATTEMPT" "被拦截后计划状态应仍为ESCALATED，实际: $PLAN_STATUS_AFTER_ATTEMPT"

# ============================================
# 测试 6: 护士提交CONNECTED自动恢复随访并保留升级历史 (核心回归验证)
# ============================================
echo ""
echo "-----------------------------------------"
echo "测试 6: 护士CONNECTED自动恢复+保留升级历史 (核心回归验证)"
echo "-----------------------------------------"

echo "  --- 6.1 提交互通记录 ---"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/records" \
    -H "Content-Type: application/json" \
    -d '{
        "planId": '"$NORMAL_PLAN_ID"',
        "callResult": "CONNECTED",
        "conversationContent": "已与患者取得联系，患者情况良好",
        "needExamReport": true,
        "examReportNote": "请携带血糖检测报告和血压监测记录",
        "nextReminderDate": "2025-02-20",
        "nextReminderNote": "下次随访复查血糖",
        "operatorName": "赵护士"
    }')
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
test_case "护士提交CONNECTED记录成功" "200" "$HTTP_STATUS" "护士提交接通记录应成功，即使计划处于升级状态"

echo "  --- 6.2 验证计划状态恢复为ACTIVE ---"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/plans/$NORMAL_PLAN_ID")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
PLAN_STATUS_AFTER_RECONNECT=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('status',''))")
CONSECUTIVE_AFTER_RECONNECT=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('consecutiveMissed',0))")

test_case "重新接通后计划状态恢复为ACTIVE" "ACTIVE" "$PLAN_STATUS_AFTER_RECONNECT" "患者重新接通后计划状态应恢复为ACTIVE，实际: $PLAN_STATUS_AFTER_RECONNECT"
test_case "重新接通后连续未接通清零" "0" "$CONSECUTIVE_AFTER_RECONNECT" "患者重新接通后连续未接通计数应为0，实际: $CONSECUTIVE_AFTER_RECONNECT"

echo "  --- 6.3 验证升级历史保留 ---"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/escalations/plan/$NORMAL_PLAN_ID/history")
BODY=$(echo "$RESPONSE" | sed '$d')
ESC_HISTORY_COUNT=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',[])))")
ESC_RESOLUTION=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); items=d.get('data',[]); print(items[0].get('resolution','') if len(items)>0 else '')")
ESC_RESOLVED=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); items=d.get('data',[]); print(items[0].get('resolved',False) if len(items)>0 else False)")

test_case "升级历史记录保留(1条)" "1" "$ESC_HISTORY_COUNT" "重新接通后升级历史应保留，实际条数: $ESC_HISTORY_COUNT"
test_case "升级记录标记为已处理" "True" "$ESC_RESOLVED" "升级记录应标记为已处理(resolved=True)，实际: $ESC_RESOLVED"
test_case "升级记录处理结果为AUTO_RESUME" "AUTO_RESUME_ON_RECONNECT" "$ESC_RESOLUTION" "升级记录的处理结果应为AUTO_RESUME_ON_RECONNECT，实际: $ESC_RESOLUTION"

echo "  --- 6.4 验证活跃升级已不存在 ---"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/escalations/plan/$NORMAL_PLAN_ID/active")
BODY=$(echo "$RESPONSE" | sed '$d')
ACTIVE_ESC_DATA=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',None))")
NO_ACTIVE_ESC="YES"
if [ "$ACTIVE_ESC_DATA" != "None" ] && [ -n "$ACTIVE_ESC_DATA" ]; then
    NO_ACTIVE_ESC="NO"
fi
test_case "重新接通后无活跃升级记录" "YES" "$NO_ACTIVE_ESC" "重新接通后应无活跃升级记录"

echo "  --- 6.5 验证升级列表不再包含该计划 ---"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/plans/escalated")
BODY=$(echo "$RESPONSE" | sed '$d')
ESCALATED_IN_LIST_AFTER=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); plans=d.get('data',[]); print(len([p for p in plans if p.get('id')==$NORMAL_PLAN_ID]))")
test_case "升级列表不再包含该计划" "0" "$ESCALATED_IN_LIST_AFTER" "恢复后升级列表不应包含该计划，实际数量: $ESCALATED_IN_LIST_AFTER"

echo "  --- 6.6 验证患者查询反映恢复后状态 ---"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/patient/110101199002025678")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
PATIENT_PLAN_STATUS=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('planStatus',''))")
PATIENT_HAS_ESC_HISTORY=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('hasEscalationHistory',False))")
PATIENT_NEXT_DATE=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('nextFollowUpDate',''))")

test_case "患者查询返回计划状态ACTIVE" "ACTIVE" "$PATIENT_PLAN_STATUS" "患者查询应反映恢复后状态为ACTIVE，实际: $PATIENT_PLAN_STATUS"
test_case "患者查询标记有升级历史" "True" "$PATIENT_HAS_ESC_HISTORY" "患者查询应标记hasEscalationHistory=True，实际: $PATIENT_HAS_ESC_HISTORY"
test_case "患者查询有下次随访日期" "HAS_DATE" "$([ -n "$PATIENT_NEXT_DATE" ] && [ "$PATIENT_NEXT_DATE" != "None" ] && echo "HAS_DATE" || echo "NO_DATE")" "患者查询应有下次随访日期，实际: $PATIENT_NEXT_DATE"

# ============================================
# 测试 7: 恢复后护士可以正常关闭计划
# ============================================
echo ""
echo "-----------------------------------------"
echo "测试 7: 恢复后护士可正常关闭计划 (回归验证)"
echo "-----------------------------------------"
REASON_ENCODED2=$(python3 -c "import urllib.parse; print(urllib.parse.quote('随访完成'))" 2>/dev/null || echo "done")
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/escalations/close-plan/$NORMAL_PLAN_ID?reason=$REASON_ENCODED2" \
    -H "Content-Type: application/json")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')

test_case "恢复后护士可以关闭计划" "200" "$HTTP_STATUS" "升级已处理，恢复后护士应能正常关闭计划"

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/plans/$NORMAL_PLAN_ID")
BODY=$(echo "$RESPONSE" | sed '$d')
PLAN_STATUS_AFTER_CLOSE=$(extract_json "$BODY" "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('status',''))")
test_case "关闭后计划状态为CLOSED" "CLOSED" "$PLAN_STATUS_AFTER_CLOSE" "护士关闭后计划状态应为CLOSED，实际: $PLAN_STATUS_AFTER_CLOSE"

# ============================================
# 汇总
# ============================================
echo ""
echo "========================================="
echo "  测试汇总"
echo "========================================="
echo "  通过: $PASSED"
echo "  失败: $FAILED"
echo ""

if [ "$FAILED" -eq 0 ]; then
    echo "✅ 所有冒烟测试通过！"
    exit 0
else
    echo "❌ 有 $FAILED 个测试失败，请检查！"
    exit 1
fi
