package com.truist.batch.reader;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.model.FileConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * ItemReader implementation for REST API data sources.
 * 
 * Fetches data from HTTP/HTTPS endpoints and converts JSON responses to Map<String, Object>.
 * Supports authentication, custom headers, and configurable timeouts.
 */
@Slf4j
public class RestApiReader implements ItemStreamReader<Map<String, Object>>, ItemStream {
    
    private final FileConfig fileConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    private Iterator<Map<String, Object>> dataIterator;
    private List<Map<String, Object>> dataList;
    private int currentIndex = 0;
    
    public RestApiReader(FileConfig fileConfig) {
        this.fileConfig = fileConfig;
        this.objectMapper = new ObjectMapper();
        this.webClient = createWebClient();
    }
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            log.info("🌐 Opening REST API connection to: {}{}", 
                fileConfig.getParams().get("baseUrl"), 
                fileConfig.getParams().get("endpoint"));
            
            // Fetch data from REST endpoint
            String response = fetchDataFromApi();
            
            // Parse JSON response to List<Map<String, Object>>
            this.dataList = parseJsonResponse(response);
            this.dataIterator = dataList.iterator();
            
            log.info("✅ Successfully loaded {} records from REST API", dataList.size());
            
        } catch (Exception e) {
            log.error("❌ Failed to open REST API connection", e);
            throw new ItemStreamException("Failed to initialize REST API reader", e);
        }
    }
    
    @Override
    public Map<String, Object> read() throws Exception {
        if (dataIterator != null && dataIterator.hasNext()) {
            Map<String, Object> item = dataIterator.next();
            currentIndex++;
            
            log.debug("📖 Reading record {}/{}: {}", currentIndex, dataList.size(), 
                item.keySet().toString());
            
            return item;
        }
        
        log.debug("📄 Finished reading {} records from REST API", currentIndex);
        return null; // End of data
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt("rest.records.read", currentIndex);
    }
    
    @Override
    public void close() throws ItemStreamException {
        log.info("🔒 Closing REST API reader. Total records processed: {}", currentIndex);
        // WebClient doesn't need explicit closing
    }
    
    /**
     * Creates WebClient with configuration from FileConfig.
     */
    private WebClient createWebClient() {
        String baseUrl = fileConfig.getParams().get("baseUrl");
        String timeout = fileConfig.getParams().getOrDefault("timeout", "30000");
        
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)); // 10MB limit
        
        // Add authentication if provided
        String authToken = fileConfig.getParams().get("authToken");
        if (authToken != null && !authToken.trim().isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, authToken);
        }
        
        return builder.build();
    }
    
    /**
     * Fetches data from the REST API endpoint.
     */
    private String fetchDataFromApi() {
        String endpoint = fileConfig.getParams().get("endpoint");
        String timeout = fileConfig.getParams().getOrDefault("timeout", "30000");
        
        log.debug("🔍 Fetching data from endpoint: {}", endpoint);
        
        return webClient
            .get()
            .uri(endpoint)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(Long.parseLong(timeout)))
            .block();
    }
    
    /**
     * Parses JSON response to List<Map<String, Object>>.
     */
    private List<Map<String, Object>> parseJsonResponse(String jsonResponse) throws Exception {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            log.warn("⚠️  Empty response from REST API");
            return List.of();
        }
        
        log.debug("📝 Parsing JSON response ({} characters)", jsonResponse.length());
        
        try {
            // Try to parse as array first
            return objectMapper.readValue(jsonResponse, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            try {
                // If not an array, try to parse as single object and wrap in list
                Map<String, Object> singleObject = objectMapper.readValue(jsonResponse, 
                    new TypeReference<Map<String, Object>>() {});
                return List.of(singleObject);
            } catch (Exception e2) {
                log.error("❌ Failed to parse JSON response as array or object", e2);
                throw new IllegalArgumentException("Invalid JSON response format", e2);
            }
        }
    }
}