CREATE DATABASE IF NOT EXISTS clinic_followup DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clinic_followup;

CREATE TABLE IF NOT EXISTS follow_up_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_name VARCHAR(50) NOT NULL,
    patient_id_card VARCHAR(20) NOT NULL,
    patient_phone VARCHAR(20),
    discharge_date DATE NOT NULL,
    disease_type VARCHAR(100) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    transfer_status VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    attending_doctor VARCHAR(50),
    assigned_nurse VARCHAR(50),
    consecutive_missed INT DEFAULT 0,
    remarks VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_patient_id_card (patient_id_card),
    INDEX idx_status (status),
    INDEX idx_transfer_status (transfer_status),
    INDEX idx_assigned_nurse (assigned_nurse),
    INDEX idx_attending_doctor (attending_doctor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS follow_up_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    follow_up_date DATE NOT NULL,
    node_order INT NOT NULL,
    description VARCHAR(200),
    completed BOOLEAN DEFAULT FALSE,
    cancelled BOOLEAN DEFAULT FALSE,
    completed_at DATETIME,
    created_at DATETIME NOT NULL,
    INDEX idx_plan_id (plan_id),
    INDEX idx_follow_up_date (follow_up_date),
    INDEX idx_plan_date (plan_id, follow_up_date),
    FOREIGN KEY (plan_id) REFERENCES follow_up_plan(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS follow_up_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    node_id BIGINT,
    call_result VARCHAR(20) NOT NULL,
    no_answer_reason VARCHAR(200),
    conversation_content VARCHAR(1000),
    next_reminder_date DATE,
    next_reminder_note VARCHAR(500),
    need_exam_report BOOLEAN DEFAULT FALSE,
    exam_report_note VARCHAR(200),
    operator_name VARCHAR(50),
    created_at DATETIME NOT NULL,
    INDEX idx_plan_id (plan_id),
    INDEX idx_node_id (node_id),
    INDEX idx_next_reminder_date (next_reminder_date),
    FOREIGN KEY (plan_id) REFERENCES follow_up_plan(id) ON DELETE CASCADE,
    FOREIGN KEY (node_id) REFERENCES follow_up_node(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS escalation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    escalation_type VARCHAR(50) NOT NULL,
    from_role VARCHAR(20) NOT NULL,
    to_doctor VARCHAR(50) NOT NULL,
    reason VARCHAR(200) NOT NULL,
    doctor_note VARCHAR(500),
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at DATETIME,
    resolution VARCHAR(500),
    created_at DATETIME NOT NULL,
    INDEX idx_plan_id (plan_id),
    INDEX idx_to_doctor (to_doctor),
    INDEX idx_resolved (resolved),
    FOREIGN KEY (plan_id) REFERENCES follow_up_plan(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
