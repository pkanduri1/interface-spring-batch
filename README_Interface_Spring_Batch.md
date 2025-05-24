
# Interface Spring Batch

A Spring Boot + Spring Batch based data transformation framework designed for ETL processing of source system files and generation of fixed-width output files. This project supports parallel execution, conditional transformations, composite field logic, and data reconciliation — all configurable via YAML files.

---

## 📁 Project Structure

```
interface-spring-batch/
├── config/               # Spring Batch Job and Step configuration classes
├── model/                # POJOs like FieldMapping, Condition, YamlMapping
├── reader/               # JdbcPagingItemReader-based readers
├── processor/            # Business logic processors (e.g., P327Processor)
├── writer/               # Fixed-width file writers
├── mapping/              # YamlMappingService for transformation orchestration
├── util/                 # Utility classes like FormatterUtil, ErrorLogger
├── resources/
│   ├── application.yml   # App-level configs
│   └── p327/             # Per-source per-job YAML transformation rules
├── BatchAuditService     # Job run auditing and reconciliation tracking
```

---

## ⚙️ Features

- ✅ Supports multiple **source systems**
- ✅ Reads from **Oracle staging tables** using **SQL Loader**
- ✅ Generates **6 fixed-width output files** including custom segments
- ✅ **Composite and conditional transformation logic** from YAML config
- ✅ **Data reconciliation** and **audit logging**
- ✅ Configurable **job skip policy** with error logging
- ✅ Runs on-prem or OpenShift

---

## 🧩 Sample YAML Mapping

Each job uses a YAML file like:

```yaml
fileType: p327
fields:
  - fieldName: LOCATION_CODE
    targetPosition: 1
    length: 6
    dataType: String
    transformationType: constant
    value: "100030"
    pad: right
    padChar: " "
  - fieldName: ACCT_NUM
    targetPosition: 2
    length: 18
    dataType: String
    transformationType: source
    sourceField: "acct_num"
```

---



---

## 🧾 Business-Driven Rule Configuration via CSV

Non-technical Business Analysts (BAs) or Product Owners (POs) define transformation rules in a structured **CSV format** containing fields like:

- `Field Name`, `Target Position`, `Length`, `Data Type`, `Transformation Type`, `Source Field`, `If`, `Else If`, `Else`, etc.

These files allow stakeholders to control mapping logic **without modifying code**.

### 🔁 CSV-to-YAML Conversion

A utility class called `CsvToYamlConverter` reads these CSV files and automatically generates YAML mapping files used by the Spring Batch engine.

```bash
java CsvToYamlConverter input_rules.csv output_rules.yml p327
```

Once converted, the YAML is plugged into the ETL pipeline to control how data is transformed per source system and job type.

---

### Example CSV Line:
```csv
Field Name,Target Position,Length,Data Type,Transformation Type,Source Field,Default Value
LOCATION_CODE,1,6,String,constant,,100030
```

Produces YAML:
```yaml
- fieldName: LOCATION_CODE
  targetPosition: 1
  length: 6
  dataType: String
  transformationType: constant
  value: "100030"
```


## 🚀 Running the Job

### In Eclipse:

1. Clone the repo and open it in Eclipse.
2. Edit `application.yml` for DB connection settings.
3. Right-click `BatchApplication.java` → Run As → Java Application.
4. Pass job parameters:
   - `sourceSystem=shaw`
   - `jobName=p327`
   - `batchDate=20240524`

### Command Line:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="sourceSystem=shaw,jobName=p327,batchDate=20240524"
```

---

## 🔒 Error Handling

- Failed rows are logged via `ErrorLogger` into a log file under `logs/errors/`.
- You can configure skip limits and log paths in `batch-properties.yml`.

---

## 🧪 Unit Testing

Tests cover:
- Formatters (e.g., numeric +9(12)V9(6))
- YAML parsing
- Processor logic
- Error handling
- `CsvToYamlConverter` utility, covering CSV parsing, YAML output, conditional logic, and error handling.

Run with:
```bash
mvn test
```

---

## 📊 Batch Audit Log

All job executions are logged in the `batch_audit_log` table with:
- Job name
- Source system
- Batch date
- File name
- Record count
- Start/End timestamps
- Error (if any)

---

## 📎 Tools & Dependencies

- Java 21
- Spring Boot 3.4.4
- Spring Batch
- Oracle JDBC
- Jackson YAML
- Lombok
- Maven

---

## 📂 Git Setup (First-Time)

```bash
git init
git remote add origin https://github.com/YOUR_USERNAME/interface-spring-batch.git
git add .
git commit -m "Initial commit"
git push -u origin main
```

---

## 👤 Author

Pavan Kanduri  
Senior Java Technical Lead  
Banking and Collections Platform Modernization
