package com.deckbuilder.mtgdeckbuilder.importer;

import com.deckbuilder.mtgdeckbuilder.application.importing.CardImportService;
import com.deckbuilder.mtgdeckbuilder.application.importing.ImportSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.function.IntConsumer;

/**
 * CLI entry point for the {@code import} profile.
 *
 * <p>
 * The exit code is the automation contract (D-031): {@code 0} success,
 * {@code 1} some rows failed, {@code 2} configuration, IO, lock or an
 * unexpected error. The exiter is injectable so tests can assert the code
 * without terminating the JVM.
 */
@Component
@Profile("import")
public class ScryfallImportRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ScryfallImportRunner.class);

	private final ImportOptions options;
	private final CardImportService importService;
	private final IntConsumer exit;

	@Autowired
	public ScryfallImportRunner(ImportOptions options, CardImportService importService) {
		this(options, importService, System::exit);
	}

	ScryfallImportRunner(ImportOptions options, CardImportService importService, IntConsumer exit) {
		this.options = options;
		this.importService = importService;
		this.exit = exit;
	}

	@Override
	public void run(ApplicationArguments args) {
		int code;
		try {
			final ImportSummary summary = importService.importAll(options);
			log.info("Import finished: {}", summary);
			code = summary.hasFailures() ? 1 : 0;
		}
		catch (Exception ex) {
			log.error("Import aborted: {}", ex.getMessage(), ex);
			code = 2;
		}
		exit.accept(code);
	}
}
