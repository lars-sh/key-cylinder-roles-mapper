package de.larssh.keycylinderroles.mapper.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.jar.Attributes.Name;

import org.apache.poi.poifs.filesystem.FileMagic;

import de.larssh.keycylinderroles.mapper.data.Cylinder;
import de.larssh.keycylinderroles.mapper.data.Key;
import de.larssh.keycylinderroles.mapper.data.KeyCylinderPermissions;
import de.larssh.keycylinderroles.mapper.sheets.csv.CsvFiles;
import de.larssh.keycylinderroles.mapper.sheets.excel.ExcelFiles;
import de.larssh.utils.Nullables;
import de.larssh.utils.io.Resources;
import de.larssh.utils.text.StringParseException;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * The CLI interface of the Key Cylinder Roles Mapper
 */
@Getter
@RequiredArgsConstructor
@Command(name = "key-cylinder-roles-mapper",
		mixinStandardHelpOptions = true,
		showDefaultValues = true,
		usageHelpWidth = 160,
		versionProvider = KeyCylinderRolesMapperCli.class)
public class KeyCylinderRolesMapperCli implements Callable<Integer>, IVersionProvider {
	/**
	 * The CLI interface of the Key Cylinder Roles Mapper
	 *
	 * @param args CLI arguments
	 */
	@SuppressWarnings("checkstyle:UncommentedMain")
	public static void main(final String... args) {
		System.exit(new CommandLine(new KeyCylinderRolesMapperCli()).execute(args));
	}

	/**
	 * Current {@link CommandSpec} instance
	 */
	@Spec
	@NonFinal
	@Nullable
	CommandSpec commandSpec;

	@NonFinal
	@Parameters(descriptionKey = "SOURCE")
	Path source = Paths.get("");

	@NonFinal
	@Parameters(descriptionKey = "DESTINATION")
	Path destination = Paths.get("");

	@Override
	public Integer call() throws IOException, StringParseException {
		final KeyCylinderPermissions sourcePermissions = read(getSource());
		final KeyCylinderPermissions destinationPermissions = read(getDestination());

		comparePermissions(sourcePermissions, destinationPermissions);
		return ExitCode.OK;
	}

	private KeyCylinderPermissions read(final Path path) throws IOException {
		return FileMagic.valueOf(path.toFile()) == FileMagic.UNKNOWN //
				? CsvFiles.read(path)
				: ExcelFiles.read(path);
	}

	@SuppressWarnings({
			"checkstyle:SuppressWarnings",
			"PMD.AvoidDeeplyNestedIfStmts",
			"PMD.CognitiveComplexity",
			"resource" })
	private void comparePermissions(final KeyCylinderPermissions source, final KeyCylinderPermissions destination) {
		final Set<Key> keys = new LinkedHashSet<>(source.getKeys());
		keys.addAll(destination.getKeys());

		final Set<Cylinder> cylinders = new LinkedHashSet<>(source.getCylinders());
		cylinders.addAll(destination.getCylinders());

		int count = 0;
		for (final Key key : keys) {
			if (!source.isIgnore(key) && !destination.isIgnore(key)) {
				for (final Cylinder cylinder : cylinders) {
					if (!source.isIgnore(cylinder) && !destination.isIgnore(cylinder)) {
						final boolean sourceAllows = source.allows(key, cylinder);
						if (sourceAllows != destination.allows(key, cylinder)) {
							final String format = sourceAllows
									? "ALT: \"%s\" (%s) soll nicht länger auf \"%s\" (%s) berechtigt sein."
									: "NEU: \"%s\" (%s) soll jetzt auf \"%s\" (%s) berechtigt werden.";

							final Key outputKey = destination.get(key).orElse(key);
							final Cylinder outputCylinder = destination.get(cylinder).orElse(cylinder);
							getStandardOutputWriter().println(String.format(format,
									outputKey.getTitle(),
									outputKey.getId(),
									outputCylinder.getTitle(),
									outputCylinder.getId()));

							count += 1;
						}
					}
				}
			}
		}

		getStandardOutputWriter().println(String.format("%d Unterschiede gefunden.", count));
	}

	private CommandSpec getCommandSpec() {
		return Nullables.orElseThrow(commandSpec);
	}

	/**
	 * Returns the standard output writer based on the current {@link CommandSpec}.
	 *
	 * @return the standard output writer
	 */
	private PrintWriter getStandardOutputWriter() {
		return getCommandSpec().commandLine().getOut();
	}

	/** {@inheritDoc} */
	@Override
	public String[] getVersion() throws IOException {
		return new String[] {
				Resources.readManifest(getClass())
						.map(manifest -> manifest.getMainAttributes().get(Name.IMPLEMENTATION_VERSION).toString())
						.orElse("unknown") };
	}

	/**
	 * Dummy to avoid the IDE to mark some fields as {@code final}.
	 */
	@SuppressWarnings("PMD.UnusedPrivateMethod")
	@SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "dummy method")
	private void nonFinalDummy() {
		source = Paths.get("");
		destination = source;
	}
}
