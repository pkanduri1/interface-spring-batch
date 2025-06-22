/* ============================================================================
   SQL SCHEMA FOR TEMPLATE TABLES
   ============================================================================ */

-- Create field_templates table
CREATE TABLE field_templates (
    file_type VARCHAR(10) NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,
    field_name VARCHAR(50) NOT NULL,
    target_position INTEGER NOT NULL,
    length INTEGER,
    data_type VARCHAR(20),
    format VARCHAR(50),
    required CHAR(1) DEFAULT 'N',
    description VARCHAR(500),
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(50),
    modified_date TIMESTAMP,
    version INTEGER DEFAULT 1,
    enabled CHAR(1) DEFAULT 'Y',
    PRIMARY KEY (file_type, transaction_type, field_name)
);

-- Create file_type_templates table  
CREATE TABLE file_type_templates (
    file_type VARCHAR(10) PRIMARY KEY,
    description VARCHAR(200),
    total_fields INTEGER,
    record_length INTEGER,
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(50),
    modified_date TIMESTAMP,
    version INTEGER DEFAULT 1,
    enabled CHAR(1) DEFAULT 'Y'
);

-- Create indexes for performance
CREATE INDEX idx_field_templates_filetype_txntype ON field_templates(file_type, transaction_type);
CREATE INDEX idx_field_templates_enabled ON field_templates(enabled);
CREATE INDEX idx_field_templates_target_position ON field_templates(file_type, transaction_type, target_position);
CREATE INDEX idx_file_type_templates_enabled ON file_type_templates(enabled);

-- Add foreign key constraint
ALTER TABLE field_templates 
ADD CONSTRAINT fk_field_templates_file_type 
FOREIGN KEY (file_type) REFERENCES file_type_templates(file_type);

-- Sample data for p327 template
INSERT INTO file_type_templates (file_type, description, total_fields, record_length, created_by)
VALUES ('p327', 'Consumer Lending Interface', 251, 2000, 'system');

INSERT INTO field_templates (file_type, transaction_type, field_name, target_position, length, data_type, format, required, description, created_by)
VALUES 
('p327', 'default', 'ACCT-NUM', 1, 18, 'String', '', 'Y', 'Account Number', 'system'),
('p327', 'default', 'CREDIT-LIMIT-AMT', 2, 13, 'Numeric', '+9(12)', 'N', 'Credit Limit Amount', 'system'),
('p327', 'default', 'EXPIRATION-DATE', 3, 8, 'Date', 'CCYYMMDD', 'N', 'Card Expiration Date', 'system'),
('p327', 'default', 'BALANCE-AMT', 4, 19, 'Numeric', '+9(12)V9(6)', 'N', 'Current Balance', 'system'),
('p327', 'default', 'OVERLIMIT-AMT', 5, 19, 'Numeric', '+9(12)V9(6)', 'N', 'Over Limit Amount', 'system');