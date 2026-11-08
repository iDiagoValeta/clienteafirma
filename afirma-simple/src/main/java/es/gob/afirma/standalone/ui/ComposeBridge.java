package es.gob.afirma.standalone.ui;

import java.io.File;
import java.io.IOException;
import java.util.List;

import es.gob.afirma.keystores.CertificateFilter;

/** Exposes the file analysis and certificate filters of the Swing sign panel to the
 * Compose UI, so both interfaces apply exactly the same signing configuration. */
public final class ComposeBridge {

	private ComposeBridge() {
		// No instanciable
	}

	/** Builds the signing configuration (signer, operation, validation, extra params) for a file.
	 * @param file File to sign.
	 * @return Signing configuration.
	 * @throws IOException If the file cannot be read. */
	public static SignOperationConfig prepareSignConfig(final File file) throws IOException {
		return SignPanel.prepareSignConfig(file);
	}

	/** Whether the configuration targets a PDF document.
	 * @param config Signing configuration.
	 * @return {@code true} for PDF files. */
	public static boolean isPdf(final SignOperationConfig config) {
		return config.getFileType() == FileType.PDF;
	}

	/** Certificate filters configured in the preferences.
	 * @return Filters, or {@code null} if none applies. */
	public static List<? extends CertificateFilter> getCertFilters() {
		return SignPanel.getCertFilters();
	}
}
