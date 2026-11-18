package com.deckbuilder.mtgdeckbuilder.importer;

import com.deckbuilder.mtgdeckbuilder.application.importing.CardImportService;
import com.deckbuilder.mtgdeckbuilder.application.importing.ImportSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Scryfall import runner")
class ScryfallImportRunnerTest {

	private final ImportOptions options = new ImportOptions();
	private final CardImportService service = mock(CardImportService.class);

	@Test
	@DisplayName("Should exit 0 when every row was imported")
	void shouldExitZeroOnSuccess() throws Exception {
		when(service.importAll(options)).thenReturn(summary(0, 0));
		final AtomicInteger code = new AtomicInteger(-1);

		new ScryfallImportRunner(options, service, code::set).run(null);

		assertThat(code).hasValue(0);
	}

	@Test
	@DisplayName("Should exit 1 when some row failed")
	void shouldExitOneOnRowFailures() throws Exception {
		when(service.importAll(options)).thenReturn(summary(1, 0));
		final AtomicInteger code = new AtomicInteger(-1);

		new ScryfallImportRunner(options, service, code::set).run(null);

		assertThat(code).hasValue(1);
	}

	@Test
	@DisplayName("Should exit 2 when the import cannot start or finish")
	void shouldExitTwoOnException() throws Exception {
		when(service.importAll(options)).thenThrow(new IOException("boom"));
		final AtomicInteger code = new AtomicInteger(-1);

		new ScryfallImportRunner(options, service, code::set).run(null);

		assertThat(code).hasValue(2);
	}

	@Test
	@DisplayName("Should describe counters and flags in the summary")
	void shouldDescribeSummary() {
		final ImportSummary summary = summary(2, 1);

		assertThat(summary.hasFailures()).isTrue();
		assertThat(summary.toString())
			.isEqualTo("cards inserted=1 updated=2 skipped=3 failed=2 obsolete=4; "
				+ "rulings staged=3 inserted=5 deleted=6 skipped=7 failed=1; "
				+ "partial=false sweepSkipped=false rulingMirrorSkipped=false; duration=12ms");
	}

	@Test
	@DisplayName("Should report success when no row failed")
	void shouldReportNoFailures() {
		assertThat(summary(0, 0).hasFailures()).isFalse();
	}

	@Test
	@DisplayName("Should build with System::exit when Spring creates the bean")
	void shouldBuildForSpring() {
		assertThat(new ScryfallImportRunner(options, service)).isNotNull();
	}

	private static ImportSummary summary(int cardsFailed, int rulingsFailed) {
		return new ImportSummary(1, 2, 3, cardsFailed, 4, 3, 5, 6, 7, rulingsFailed,
				false, false, false, Duration.ofMillis(12));
	}
}
