package com.truist.batch.adapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.truist.batch.model.FileConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry that discovers and manages DataSourceAdapter implementations.
 * 
 * Auto-discovers all Spring beans implementing DataSourceAdapter and provides
 * lookup functionality based on format support and priority.
 */
@Component
@Slf4j
public class DataSourceAdapterRegistry {

	private final Map<String, DataSourceAdapter> adaptersByFormat = new ConcurrentHashMap<>();
	private final List<DataSourceAdapter> allAdapters;

	/**
	 * Constructor auto-wires all DataSourceAdapter beans and builds format mapping.
	 * 
	 * @param adapters All DataSourceAdapter implementations found by Spring
	 */
	@Autowired
	public DataSourceAdapterRegistry(List<DataSourceAdapter> adapters) {
		this.allAdapters = adapters;

		System.out
				.println("🚀 FIXED DataSourceAdapterRegistry constructor called with " + adapters.size() + " adapters");
		log.info("🔌 Discovered {} data source adapters:", adapters.size());

		// Build format-to-adapter mapping, respecting priority
		for (DataSourceAdapter adapter : adapters) {
			log.info("  📌 {} (priority: {})", adapter.getAdapterName(), adapter.getPriority());

			// Register adapter for all formats it supports
			registerAdapterFormats(adapter);
		}

		System.out.println("🎯 FIXED Adapter registry initialized with " + adaptersByFormat.size()
				+ " format mappings: " + adaptersByFormat.keySet());
		log.info("🎯 Adapter registry initialized with {} format mappings", adaptersByFormat.size());
	}

	/**
	 * Gets the appropriate adapter for the specified format.
	 * 
	 * @param format The format identifier (e.g., "jdbc", "rest", "kafka")
	 * @return DataSourceAdapter that supports the format
	 * @throws IllegalArgumentException if no adapter supports the format
	 */
	public DataSourceAdapter getAdapter(String format) {
		if (format == null || format.trim().isEmpty()) {
			throw new IllegalArgumentException("Format cannot be null or empty");
		}

		DataSourceAdapter adapter = adaptersByFormat.get(format.toLowerCase());
		if (adapter == null) {
			String supportedFormats = adaptersByFormat.keySet().stream().sorted().collect(Collectors.joining(", "));

			throw new IllegalArgumentException(String
					.format("No adapter found for format '%s'. Supported formats: [%s]", format, supportedFormats));
		}

		log.debug("🔍 Selected adapter {} for format '{}'", adapter.getAdapterName(), format);
		return adapter;
	}

	/**
	 * Creates and validates a reader for the given configuration.
	 * 
	 * @param fileConfig Complete file configuration
	 * @return Configured ItemReader
	 * @throws IllegalArgumentException if configuration is invalid
	 */
	public ItemReader<Map<String, Object>> createReader(FileConfig fileConfig) {
		String format = fileConfig.getParams().get("format");
		if (format == null) {
			throw new IllegalArgumentException("FileConfig must specify 'format' parameter");
		}

		DataSourceAdapter adapter = getAdapter(format);

		// Validate configuration before creating reader
		try {
			adapter.validateConfiguration(fileConfig);
		} catch (Exception e) {
			throw new IllegalArgumentException(
					String.format("Invalid configuration for format '%s': %s", format, e.getMessage()), e);
		}

		return adapter.createReader(fileConfig);
	}

	/**
	 * Gets all registered adapters for diagnostics.
	 * 
	 * @return List of all discovered adapters
	 */
	public List<DataSourceAdapter> getAllAdapters() {
		return List.copyOf(allAdapters);
	}

	/**
	 * Gets all supported formats.
	 * 
	 * @return Set of supported format identifiers
	 */
	public java.util.Set<String> getSupportedFormats() {
		return adaptersByFormat.keySet();
	}

	/**
	 * Registers an adapter for all formats it supports, handling priority
	 * conflicts.
	 */
	private void registerAdapterFormats(DataSourceAdapter adapter) {
		// ✅ FIXED: Extended format list to include 'database', 'sql', etc.
		String[] commonFormats = { "jdbc", "database", "sql", // Database formats - FIXED: added 'database'
				"rest", "api", "http", "https", // REST API formats
				"kafka", "stream", // Streaming formats
				"s3", "aws", // Cloud storage formats
				"csv", "excel", "json", "xml" // File formats
		};

		System.out.println("🔍 FIXED Testing adapter " + adapter.getAdapterName() + " against " + commonFormats.length
				+ " formats");

		for (String format : commonFormats) {
			boolean supportsFormat = adapter.supports(format);
			System.out.println("  " + adapter.getAdapterName() + " supports '" + format + "': " + supportsFormat);

			if (supportsFormat) {
				DataSourceAdapter existing = adaptersByFormat.get(format);

				if (existing == null || adapter.getPriority() > existing.getPriority()) {
					adaptersByFormat.put(format, adapter);
					System.out.println(
							"  ✅ FIXED Registered " + adapter.getAdapterName() + " for format '" + format + "'");

					if (existing != null) {
						log.info("  🔄 {} superseded {} for format '{}' (priority {} > {})", adapter.getAdapterName(),
								existing.getAdapterName(), format, adapter.getPriority(), existing.getPriority());
					}
				} else {
					System.out.println("  ⏭️  FIXED Skipped " + adapter.getAdapterName() + " for format '" + format
							+ "' (lower priority than " + existing.getAdapterName() + ")");
				}
			}
		}

		System.out.println("🎯 FIXED Adapter " + adapter.getAdapterName() + " registration complete");
	}
}