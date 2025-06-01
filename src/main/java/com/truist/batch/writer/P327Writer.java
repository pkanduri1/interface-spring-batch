/*
 * package com.truist.batch.writer;
 * 
 * import java.io.BufferedWriter; import java.io.FileWriter; import
 * java.io.IOException; import java.util.List; import java.util.Map;
 * 
 * import org.slf4j.Logger; import org.slf4j.LoggerFactory; import
 * org.springframework.batch.item.Chunk; import
 * org.springframework.batch.item.ItemWriter; import
 * org.springframework.beans.factory.annotation.Value;
 * 
 * import com.truist.batch.mapping.YamlMappingService; import
 * com.truist.batch.model.FieldMapping; import
 * com.truist.batch.util.FormatterUtil;
 * 
 * public class P327Writer implements ItemWriter<Map<String, String>> {
 * 
 * private static final Logger logger =
 * LoggerFactory.getLogger(P327Writer.class); private final String
 * outputFilePath; private final List<Map.Entry<String, FieldMapping>>
 * fieldMappings;
 * 
 * private BufferedWriter writer;
 * 
 * public P327Writer(@Value("#{jobParameters[ 'sourceSystem']]") String
 * sourceSystem,
 * 
 * @Value("#(jobParameters[' jobName ']}") String jobName, YamlMappingService
 * yamlMappingService) { this.outputFilePath =
 * "C: \\app\\Software\\CACS\\ftp\\input\\" + sourceSystem + "\\" + jobName + "\
 * \" + sourceSystem + "_" + FormatterUtil.getToday() + ".txt"; String ymlPath =
 * jobName + "/" + sourceSystem + "/" + jobName + ".yml"; this.fieldMappings =
 * yamlMappingService.loadFieldMappings(ymlPath); }
 * 
 * @Override public void write(Chunk<? extends Map<String, String>> chunk)
 * throws Exception { if (null == writer) { initializeWriter(); } for
 * (Map<String, String> item : chunk) { StringBuilder sb = new StringBuilder();
 * for (Map.Entry<String, FieldMapping> en : fieldMappings) { String key =
 * en.getKey(); String val = item.getOrDefault(key,""); sb.append(val);
 * 
 * } writer.write(sb.toString()); writer.newLine(); // Implement actual writing
 * logic here, e.g., write to file or buffer logger.debug("Writing item: {}",
 * item); } }
 * 
 * private void initializeWriter() throws IOException { this.writer = new
 * BufferedWriter(new FileWriter(outputFilePath, false)); } }
 */