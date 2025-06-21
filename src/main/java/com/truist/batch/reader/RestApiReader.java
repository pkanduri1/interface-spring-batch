package com.truist.batch.reader;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truist.batch.model.FileConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestApiReader implements ItemStreamReader<Map<String, Object>>, ItemStream {
    
    private final FileConfig fileConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private Iterator<Map<String, Object>> dataIterator;
    private List<Map<String, Object>> dataList;
    private int currentIndex = 0;
    
    public RestApiReader(FileConfig fileConfig) {
        this.fileConfig = fileConfig;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            log.info("🌐 Opening REST API connection to: {}{}", 
                fileConfig.getParams().get("baseUrl"), 
                fileConfig.getParams().get("endpoint"));
            
            String response = fetchDataFromApi();
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
            return item;
        }
        return null;
    }
    
    private String fetchDataFromApi() {
        String baseUrl = fileConfig.getParams().get("baseUrl");
        String endpoint = fileConfig.getParams().get("endpoint");
        String url = baseUrl + endpoint;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        
        String authToken = fileConfig.getParams().get("authToken");
        if (authToken != null && !authToken.trim().isEmpty()) {
            headers.set("Authorization", authToken);
        }
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        
        return response.getBody();
    }
    
    private List<Map<String, Object>> parseJsonResponse(String jsonResponse) throws Exception {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            Map<String, Object> singleObject = objectMapper.readValue(jsonResponse, 
                new TypeReference<Map<String, Object>>() {});
            return List.of(singleObject);
        }
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt("rest.records.read", currentIndex);
    }
    
    @Override
    public void close() throws ItemStreamException {
        log.info("🔒 Closing REST API reader. Total records processed: {}", currentIndex);
    }
}