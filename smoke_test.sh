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

wait_for_service

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
NODES_COUNT=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',{}).get('nodes',[])))" 2>/dev/null || echo "ERROR")

test_case "转院患者计划创建" "200" "$HTTP_STATUS" "创建转院患者的随访计划"
test_case "转院患者无随访节点" "0" "$NODES_COUNT" "已转院患者不应生成随访节点，实际节点数: $NODES_COUNT"

TRANSFER_PLAN_ID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('id',''))" 2>/dev/null)

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
NODES_COUNT=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',{}).get('nodes',[])))" 2>/dev/null || echo "ERROR")
RISK_INTERVAL=$(echo "$BODY" | python3 -c "
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
" 2>/dev/null || echo "ERROR")

test_case "正常患者计划创建" "200" "$HTTP_STATUS" "创建未转院患者的随访计划"
test_case "正常患者生成4个随访节点" "4" "$NODES_COUNT" "未转院患者应生成4个随访节点，实际节点数: $NODES_COUNT"
test_case "中风险间隔7天" "7" "$RISK_INTERVAL" "中风险患者随访间隔应为7天，实际间隔: $RISK_INTERVAL 天"

NORMAL_PLAN_ID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('id',''))" 2>/dev/null)

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
ERROR_MSG=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('message',''))" 2>/dev/null || echo "ERROR")

test_case "日期错误被后端拦截" "400" "$HTTP_STATUS" "复诊日期(2025-01-10)早于出院日期(2025-01-15)应被拦截，错误信息: $ERROR_MSG"

echo ""
echo "-----------------------------------------"
echo "测试 4: 连续未联系达到阈值自动升级"
echo "-----------------------------------------"

for i in 1 2 3; do
    RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/records" \
        -H "Content-Type: application/json" \
        -d '{
            "planId": '"$NORMAL_PLAN_ID"',
            "callResult": "NO_ANSWER",
            "noAnswerReason": "电话无人接听",
            "operatorName": "李护士"
        }')
    HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
    echo "  第 $i 次未接通记录: HTTP $HTTP_STATUS"
done

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/plans/$NORMAL_PLAN_ID")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
PLAN_STATUS=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('status',''))" 2>/dev/null || echo "ERROR")
CONSECUTIVE_MISSED=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('consecutiveMissed',0))" 2>/dev/null || echo "ERROR")

test_case "连续3次未接通后计划状态变为ESCALATED" "ESCALATED" "$PLAN_STATUS" "连续3次未接通后计划应自动升级，实际状态: $PLAN_STATUS"
test_case "连续未接通计数为3" "3" "$CONSECUTIVE_MISSED" "连续未接通计数应为3，实际: $CONSECUTIVE_MISSED"

echo ""
echo "-----------------------------------------"
echo "测试 5: 升级后护士不能直接关闭计划"
echo "-----------------------------------------"
REASON_ENCODED=$(python3 -c "import urllib.parse; print(urllib.parse.quote('测试关闭'))" 2>/dev/null || echo "test")
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/escalations/close-plan/$NORMAL_PLAN_ID?reason=$REASON_ENCODED" \
    -H "Content-Type: application/json")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
ERROR_MSG=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('message',''))" 2>/dev/null || echo "ERROR")

test_case "升级后护士关闭被拦截" "400" "$HTTP_STATUS" "存在未处理升级记录时护士不能直接关闭计划，错误信息: $ERROR_MSG"

echo ""
echo "-----------------------------------------"
echo "测试 6: 患者重新接通恢复随访但保留升级历史"
echo "-----------------------------------------"

ESCALATION_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/escalations/plan/$NORMAL_PLAN_ID/active")
ESCALATION_HTTP_STATUS=$(echo "$ESCALATION_RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
ESCALATION_BODY=$(echo "$ESCALATION_RESPONSE" | sed '$d')
ESCALATION_ID=$(echo "$ESCALATION_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('id',''))" 2>/dev/null)

if [ -n "$ESCALATION_ID" ] && [ "$ESCALATION_ID" != "None" ]; then
    RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/escalations/resolve" \
        -H "Content-Type: application/json" \
        -d '{
            "escalationId": '"$ESCALATION_ID"',
            "doctorNote": "已与患者取得联系",
            "resolution": "恢复随访",
            "resumeFollowUp": true
        }')
    HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
    test_case "医生处理升级并恢复随访" "200" "$HTTP_STATUS" "医生处理升级记录，恢复随访状态"
fi

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/plans/$NORMAL_PLAN_ID")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
PLAN_STATUS=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('status',''))" 2>/dev/null || echo "ERROR")
CONSECUTIVE_MISSED=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('consecutiveMissed',0))" 2>/dev/null || echo "ERROR")

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/escalations/plan/$NORMAL_PLAN_ID/history")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
HISTORY_COUNT=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',[])))" 2>/dev/null || echo "ERROR")

test_case "恢复后计划状态为ACTIVE" "ACTIVE" "$PLAN_STATUS" "患者重新接通后计划状态应恢复为ACTIVE，实际: $PLAN_STATUS"
test_case "连续未接通计数重置为0" "0" "$CONSECUTIVE_MISSED" "患者重新接通后连续未接通计数应重置为0，实际: $CONSECUTIVE_MISSED"
test_case "升级历史保留" "1" "$HISTORY_COUNT" "升级历史应保留，历史记录数: $HISTORY_COUNT"

echo ""
echo "-----------------------------------------"
echo "测试 7: 患者端查询随访信息"
echo "-----------------------------------------"
RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/patient/110101199002025678")
HTTP_STATUS=$(echo "$RESPONSE" | tail -1 | sed 's/.*HTTP_STATUS://')
BODY=$(echo "$RESPONSE" | sed '$d')
NEXT_DATE=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('nextFollowUpDate',''))" 2>/dev/null || echo "ERROR")

test_case "患者端查询成功" "200" "$HTTP_STATUS" "患者通过身份证号查询随访信息"
test_case "患者能看到下次随访日期" "NOT_EMPTY" "$( [ -n "$NEXT_DATE" ] && echo "NOT_EMPTY" || echo "EMPTY" )" "下次随访日期: $NEXT_DATE"

echo ""
echo "========================================="
echo "  测试结果汇总"
echo "========================================="
echo "  通过: $PASSED"
echo "  失败: $FAILED"
echo "========================================="

if [ "$FAILED" -gt 0 ]; then
    echo ""
    echo "❌ 存在测试失败，请检查相关功能"
    exit 1
else
    echo ""
    echo "✅ 所有冒烟测试通过！"
    exit 0
fi
