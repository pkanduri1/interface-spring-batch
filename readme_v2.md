# Interface Spring Batch

**A configuration-driven, low-code batch processing framework** built on Spring Boot 3.4.4 and Spring Batch 5.x.  
It ingests delinquent-account data from multiple source systems, applies field-level mappings, and produces fixed-width output files for downstream consumption.

---

## Table of Contents

1. [Introduction](#introduction)  
2. [Key Features](#key-features)  
3. [Architecture Overview](#architecture-overview)  
4. [Getting Started](#getting-started)  
   - [Prerequisites](#prerequisites)  
   - [Configuration Files](#configuration-files)  
5. [Mapping Definitions](#mapping-definitions)  
   - [Standard CSV Format](#standard-csv-format)  
   - [Multi-Transaction Type Support](#multi-transaction-type-support)
   - [Generating YAML from CSV](#generating-yaml-from-csv)  
6. [Running the Batch](#running-the-batch)  
7. [Extending the Framework](#extending-the-framework)  
8. [Testing](#testing)  
9. [Troubleshooting](#troubleshooting)  
10. [Recent Enhancements](#recent-enhancements)
11. [License](#license)

---

## Introduction

This project provides a generic batch interface to:

- **Integrate** with over 20 source systems (e.g. SHAW, HR, DDA).  
- **Ingest** input data from Oracle staging tables (populated by SQL*Loader), CSV, pipe-delimited, Excel, or fixed-width files.  
- **Transform** each record using configurable field mappings stored in YAML.  
- **Write** six standardized fixed-width output files per source system.  
- **Partition** work dynamically by transaction types for parallel processing.  
- **Audit & Reconcile** counts at both step and job level.

Non-technical stakeholders can define or update field mappings in a simple CSV and regenerate the YAML in one click—no Java code changes required.

---

## Key Features

- **Configuration-First**: All logic driven by `application.yml`, per-source YAMLs, and CSV-to-YAML mapping files.  
- **Low Code**: Business Analysts (BAs) update mappings via CSV → YAML without developer involvement.  
- **Multi-Transaction Type Support**: Single job can process multiple transaction types with different SQL queries and output files.
- **Dynamic Partitioning**: Automatically splits work per `transactionType` in your mappings for parallel performance.  
- **Multiple Input Formats**: JDBC, CSV, pipe-delimited, Excel, fixed-width.  
- **Rich Transformations**: Constants, direct source fields, composite (sum/concat), and conditional (if-then-else) logic.  
- **Full Auditing**: Persists step- and job-level metrics to audit tables for reconciliation.  
- **Repeatable & Idempotent**: Same input + config → same output; supports restart on failure.

---

## Architecture Overview

1. **Tasklet** loads the latest `batchDate` from `CM3INT.BATCH_DATE_LOCATOR` by `sourceSystem`.  
2. **Generic Partitioner** reads `FileConfig` entries, loads all YAML mapping docs, and creates one partition per `transactionType`.  
3. **Chunk Step** (`genericStep`):  
   - **Reader**: `GenericReader` delegates to `JdbcRecordReader` or file readers based on `format` param.  
   - **Processor**: `GenericProcessor` applies YAML-driven field mappings in defined order.  
   - **Writer**: `GenericWriter` concatenates padded strings into fixed-width lines.  
4. **Listeners** capture metrics and insert rows into `BATCH_STEP_AUDIT` and `BATCH_JOB_AUDIT`.  

### Transaction Type Partitioning

For jobs with multiple transaction types (e.g., atoctran with types 200 and 900):

```
Partitioner → [200 partition] → Reader → Processor → Writer → atoctran_200.txt
           → [900 partition] → Reader → Processor → Writer → atoctran_900.txt
```

Each partition processes its specific transaction type with dedicated SQL query and YAML mapping.

---

## Getting Started

### Prerequisites

- Java 17+  
- Maven or Gradle  
- Oracle client for SQL*Loader (optional if staging tables pre-populated)  
- Linux environment for file paths (adjustable for Windows)

### Configuration Files

- **`application.yml`**: global defaults, gridSize, chunkSize, and paths.  
- **`batch-sources/*.yml`**: per-source definitions (inputBasePath, queries, outputPath).  
- **`src/main/resources/mappings/*.yml`**: field mappings generated from CSV.

---

## Mapping Definitions

### Standard CSV Format

Each mapping CSV must have the following header columns (case-insensitive):

| Column             | Description                                                                         |
|--------------------|-------------------------------------------------------------------------------------|
| FieldName          | Unique key for the mapping (ignored at runtime).                                    |
| SourceFieldName    | Name of the source column (empty for constants).                                    |
| TargetFieldName    | Name of the field in the output file.                                               |
| TargetPosition     | 1-based ordinal position for output order.                                         |
| Length             | Fixed width length of the field (in characters).                                    |
| DataType           | `String`, `Numeric`, or `Date`.                                                     |
| Format             | E.g. `"CCYYMMDD"`, `"9(12)"`, or `"+9(12)V9(6)"` for numeric.                        |
| Pad                | `left` or `right` padding alignment.                                               |
| TransformationType | `constant`, `source`, `composite`, or `conditional`.                                |
| Transform          | For composite: `sum` or `concat` (ignored otherwise).                               |
| DefaultValue       | Fallback when source value is null or missing.                                      |
| IfExpr             | Top-level `if` expression for conditional mappings (optional).                      |
| ElseIfExpr         | `else-if` expression(s) (optional, can be repeated).                                |
| Else               | `else` expression (optional).                                                       |
| TransactionType    | Partition key; if omitted or blank, treated as `default`.                           |

#### Example CSV

```csv
FieldName,SourceFieldName,TargetFieldName,TargetPosition,Length,DataType,Format,Pad,TransformationType,Transform,DefaultValue,IfExpr,ElseIfExpr,Else,TransactionType
LOCATION-CODE,,LOCATION-CODE,1,6,String,,right,constant,,100020,,,,
ACCT-NUM,ACCT_NUM,ACCT-NUM,2,18,String,,right,source,,,"",,
BALANCE-AMT,,BALANCE-AMT,5,19,Numeric,"+9(12)V9(6)",left,constant,,0,,,,
```

### Multi-Transaction Type Support

For jobs processing multiple transaction types, create separate CSV rows or YAML documents for each type:

```csv
# Transaction 200 mappings
LOCATION-CODE,,LOCATION-CODE,1,6,String,,right,constant,,100020,,,,"200"
ACCT-NUM,acct_num,ACCT-NUM,2,18,String,,right,source,,,,,,,"200"

# Transaction 900 mappings  
LOCATION-CODE,,LOCATION-CODE,1,6,String,,right,constant,,100020,,,,"900"
TRANSACTION-AMT,net_pay,TRANSACTION-AMT,5,13,Numeric,"9(12)",left,source,,,,,,,"900"
```

Configuration in `batch-sources/{source}-batch-props.yml`:

```yaml
jobs:
  atoctran:
    files:
      - transactionType: "200"
        outputPath: ${batch.defaults.outputBasePath}/hr/atoctran_200_${DATE}.txt
        params:
          query: SELECT '100020' as location_code, acct_num, '200' as transaction_type FROM HR_MASTER...
      
      - transactionType: "900"  
        outputPath: ${batch.defaults.outputBasePath}/hr/atoctran_900_${DATE}.txt
        params:
          query: SELECT '100020' as location_code, net_pay, '900' as transaction_type FROM HR_MASTER...
```

### Generating YAML from CSV

Use the included `CsvToYamlConverter`:

```bash
mvn exec:java \
  -Dexec.mainClass="com.truist.batch.util.CsvToYamlConverter" \
  -Dexec.args="p327 hr"
```

This reads `/resources/mappings/p327-hr-mapping.csv` and writes `/resources/mappings/p327/hr/p327.yml`, creating one YAML document per `TransactionType`.

---

## Running the Batch

Build and run:

```bash
mvn clean package
java -jar target/interface-batch.jar \
  --sourceSystem=hr \
  --jobName=p327
```

Since `batchDate` is loaded from `CM3INT.BATCH_DATE_LOCATOR`, only `sourceSystem` and `jobName` are required.

### Example Multi-Transaction Output

For atoctran job with HR source system:
- `atoctran_200_20250601.txt` - Transaction type 200 records
- `atoctran_900_20250601.txt` - Transaction type 900 records

---

## Extending the Framework

1. **Add a new source system**:  
   - Create `batch-sources/{source}.yml` with connection/query/output settings.  
   - Drop mapping CSV in `resources/mappings/{fileType}-{source}-mapping.csv`.  
   - Run the converter to generate YAML.  

2. **Add a new output file type**:  
   - Add a new jobName block in `application.yml` or source-specific YAML.  
   - Provide a mapping CSV and generate the YAML.

3. **Add multi-transaction type support**:
   - Define multiple `transactionType` entries in CSV mapping file.
   - Configure separate file entries with unique `transactionType` and `outputPath` in batch config.
   - Framework automatically creates partitions per transaction type.

---

## Testing

- **Unit tests**: processor, reader, writer, and mapping service cover core logic.  
- **Integration tests**: H2-based tests for JDBC readers and full end-to-end job runs via `JobLauncherTestUtils`.

---

## Troubleshooting

- **Empty output**: verify staging tables, `batchDate`, and `TransactionType` filters.  
- **Mapping errors**: check YAML syntax and `FieldMapping` definitions; logs run at DEBUG under `com.truist.batch`.  
- **Performance**: tune `batch.gridSize` and `batch.chunkSize` in `application.yml`.
- **Field name mismatches**: SQL returns uppercase fields but YAML expects exact case - framework handles case-insensitive matching.
- **Partition collisions**: Ensure each transaction type has unique `outputPath` configuration.

---

## Recent Enhancements

### Multi-Transaction Type Architecture (v2.0)

- **Enhanced Partitioning**: `GenericPartitioner` creates 1:1 partitions for configured transaction types instead of scanning YAML documents.
- **Multi-Document YAML Support**: `YamlMappingService.getMapping()` properly handles transaction-type-specific mappings.
- **Case-Insensitive Field Resolution**: Automatic handling of Oracle's uppercase field names vs YAML lowercase expectations.
- **Improved Error Handling**: Better validation and debugging for complex transformation scenarios.

### Atoctran Implementation

Successfully implemented atoctran batch job processing multiple transaction types (200, 900) with:
- Separate SQL queries per transaction type
- Independent output files with date stamps
- Transaction-specific field mappings
- Parallel partition processing

### Key Architecture Improvements

1. **Multi-document YAML**: Use `getMapping()` instead of `loadFieldMappings()` for transaction-type partitioning
2. **Partition Design**: Create partitions 1:1 with file configs, eliminating duplicates  
3. **Case Sensitivity**: Oracle returns uppercase field names - implemented case-insensitive matching
4. **Data Flow**: Processor transforms, Writer outputs - eliminates double transformation
5. **Field Validation**: Early validation of required formats with graceful error skipping

---

## Future Updates

We plan to introduce the following enhancements:

- **UI-Based Configuration**  
  A web-based interface where Business Analysts and Product Owners can edit source-system and job mappings directly without touching YAML or CSV files. Changes will be validated in real time and versioned automatically.

- **Automated SQL*Loader Control File Generation**  
  A utility to generate Oracle SQL*Loader control files from the same mapping definitions, so that raw source files can be loaded into staging tables without manually writing `.ctl` files.

- **Dynamic Scheduling & Monitoring Dashboard**  
  Integration with a scheduling engine (e.g., Quartz or Kubernetes CronJobs) and a live dashboard to track job runs, failures, and audit metrics.

- **Enhanced Expression Builder**  
  A drag-and-drop builder for conditional and composite transformations, reducing the need to write expressions by hand.

- **Plugin Framework for New Formats**  
  A lightweight plugin mechanism to add support for new input formats (JSON, XML, Avro) and output targets (Kafka, S3) without core code changes.

---

## License

MIT License – see [LICENSE](LICENSE.md) for details.