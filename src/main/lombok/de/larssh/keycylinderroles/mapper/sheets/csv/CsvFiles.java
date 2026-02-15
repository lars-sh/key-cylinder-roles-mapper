package de.larssh.keycylinderroles.mapper.sheets.csv;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import de.larssh.keycylinderroles.mapper.data.Cylinder;
import de.larssh.keycylinderroles.mapper.data.Key;
import de.larssh.keycylinderroles.mapper.data.KeyCylinderPermissions;
import de.larssh.utils.Finals;
import de.larssh.utils.Optionals;
import de.larssh.utils.annotations.PackagePrivate;
import de.larssh.utils.text.Csv;
import de.larssh.utils.text.CsvRow;
import de.larssh.utils.text.Strings;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CsvFiles {
	private static final char CSV_SEPARATOR = Finals.constant(';');

	private static final char CSV_ESCAPER = Finals.constant('"');

	private static final int COLUMN_CYLINDER_BUILDING = 0;

	private static final int COLUMN_CYLINDER_ID = 1;

	private static final int COLUMN_CYLINDER_NAME = 2;

	private static final int ROW_KEY_GROUP = 0;

	private static final int ROW_KEY_FIRST_NAME = 1;

	private static final int ROW_KEY_LAST_NAME = 2;

	private static final int ROW_KEY_ID = 4;

	private static Charset determineCharset(final Path path) throws IOException {
		if (System.currentTimeMillis() > 0) {
			return StandardCharsets.UTF_16LE;
		}

		// SimonVoss Locking System Management seems to use UTF-16 LE by default
		try (InputStream inputStream = Files.newInputStream(path)) {
			inputStream.read();
			if (inputStream.read() == '\0') {
				return StandardCharsets.UTF_16LE;
			}
		}
		return StandardCharsets.UTF_8;
	}

	public static KeyCylinderPermissions read(final Path path) throws IOException {
		try (Reader reader = Files.newBufferedReader(path, determineCharset(path))) {
			final Csv csv = Csv.parse(reader, CSV_SEPARATOR, CSV_ESCAPER);
			return new CsvFileReader(csv).read();
		}
	}

	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	private static class CsvFileReader {
		private static OptionalInt getFirstNonBlank(final List<String> values) {
			int index = 0;
			for (final String value : values) {
				if (!Strings.isBlank(value)) {
					return OptionalInt.of(index);
				}
				index += 1;
			}
			return OptionalInt.empty();
		}

		private static int getFirstNonBlankColumn(final Csv csv, final int column) {
			return csv.stream().allMatch(row -> row.size() <= column || Strings.isBlank(row.get(column)))
					? getFirstNonBlankColumn(csv, column + 1)
					: column;
		}

		private static Optional<CsvRow> getFirstNonBlankRow(final Csv csv) {
			return csv.stream().filter(row -> !row.stream().allMatch(Strings::isBlank)).findFirst();
		}

		private static Optional<String> getValue(final List<String> values, final int index) {
			return values.size() <= index ? Optional.empty() : Optionals.ofNonBlank(values.get(index));
		}

		Csv csv;

		@PackagePrivate
		KeyCylinderPermissions read() {
			final Map<Cylinder, Integer> cylinders = getCylinders();
			final Map<Key, Integer> keys = getKeys();
			return getPermissions(cylinders, keys);
		}

		private Map<Cylinder, Integer> getCylinders() {
			final int firstFilledColumn = getFirstNonBlankColumn(csv, 0);

			final OptionalInt firstCylinderRow = getFirstNonBlank(
					csv.stream().map(row -> getValue(row, firstFilledColumn).orElse("")).collect(toList()));
			if (!firstCylinderRow.isPresent()) {
				return emptyMap();
			}

			return getCylinders(firstFilledColumn, firstCylinderRow.getAsInt());
		}

		private Map<Cylinder, Integer> getCylinders(final int firstFilledColumn, final int firstCylinderRow) {
			final Map<Cylinder, Integer> cylinderRows = new LinkedHashMap<>();
			final int numberOfRows = csv.size();
			for (int row = firstCylinderRow; row < numberOfRows; row += 1) {
				cylinderRows.put(createCylinder(firstFilledColumn, row), row);
			}
			return unmodifiableMap(cylinderRows);
		}

		private Cylinder createCylinder(final int column, final int rowIndex) {
			final CsvRow row = csv.get(rowIndex);

			final String id = getValue(row, column + COLUMN_CYLINDER_ID).orElseThrow(); // TODO
			final Optional<String> name = getValue(row, column + COLUMN_CYLINDER_NAME);
			final Optional<String> building = getValue(row, column + COLUMN_CYLINDER_BUILDING);

			return new Cylinder(id, name.orElse(""), Optional.empty(), building, false);
		}

		private Map<Key, Integer> getKeys() {
			final Optional<CsvRow> firstFilledRow = getFirstNonBlankRow(csv);
			if (!firstFilledRow.isPresent()) {
				return emptyMap();
			}

			final OptionalInt firstKeyColumn = Optionals.flatMapToInt(firstFilledRow, CsvFileReader::getFirstNonBlank);
			if (!firstKeyColumn.isPresent()) {
				return emptyMap();
			}

			return getKeys(firstKeyColumn.getAsInt(), firstFilledRow.get());
		}

		private Map<Key, Integer> getKeys(final int firstKeyColumn, final CsvRow firstFilledRow) {
			final Map<Key, Integer> keyColumns = new LinkedHashMap<>();
			final int numberOfColumns = firstFilledRow.size();
			for (int column = firstKeyColumn; column < numberOfColumns; column += 1) {
				keyColumns.put(createKey(column, firstFilledRow.getRowIndex()), column);
			}
			return unmodifiableMap(keyColumns);
		}

		private Key createKey(final int column, final int row) {
			final String id = getValue(csv.get(row + ROW_KEY_ID), column).orElseThrow(); // TODO
			final Optional<String> lastName = getValue(csv.get(row + ROW_KEY_LAST_NAME), column);
			final Optional<String> firstName = getValue(csv.get(row + ROW_KEY_FIRST_NAME), column);
			final Optional<String> group = getValue(csv.get(row + ROW_KEY_GROUP), column);

			return new Key(id, Optional.empty(), lastName, firstName, group, false);
		}

		private KeyCylinderPermissions getPermissions(final Map<Cylinder, Integer> cylinders,
				final Map<Key, Integer> keys) {
			final Map<Key, Set<Cylinder>> permissions = new HashMap<>();
			for (final Entry<Key, Integer> key : keys.entrySet()) {
				for (final Entry<Cylinder, Integer> cylinder : cylinders.entrySet()) {
					if (getValue(csv.get(cylinder.getValue()), key.getValue()).isPresent()) {
						permissions.computeIfAbsent(key.getKey(), k -> new HashSet<>()).add(cylinder.getKey());
					}
				}
			}
			return new KeyCylinderPermissions(keys.keySet(), cylinders.keySet(), permissions);
		}
	}
}
