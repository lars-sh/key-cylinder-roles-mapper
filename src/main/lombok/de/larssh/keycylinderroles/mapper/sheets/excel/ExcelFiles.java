package de.larssh.keycylinderroles.mapper.sheets.excel;

import static de.larssh.utils.Collectors.toLinkedHashMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.hssf.usermodel.HSSFWorkbookFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;

import de.larssh.keycylinderroles.mapper.data.Cylinder;
import de.larssh.keycylinderroles.mapper.data.Key;
import de.larssh.keycylinderroles.mapper.data.KeyCylinderPermissions;
import de.larssh.keycylinderroles.mapper.utils.CellValues;
import de.larssh.keycylinderroles.mapper.utils.Workbooks;
import de.larssh.utils.Nullables;
import de.larssh.utils.OptionalInts;
import de.larssh.utils.Optionals;
import de.larssh.utils.annotations.PackagePrivate;
import de.larssh.utils.text.Strings;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExcelFiles {
	static {
		// Making sure that both Workbook Factories are registered to support XLS and
		// XLSX. Registering automatically might be a problem when creating a JAR with
		// all dependencies.
		WorkbookFactory.addProvider(new XSSFWorkbookFactory());
		WorkbookFactory.addProvider(new HSSFWorkbookFactory());
	}

	public static KeyCylinderPermissions read(final Path path) throws IOException {
		try (InputStream inputStream = Files.newInputStream(path);
				Workbook workbook = WorkbookFactory.create(inputStream)) {
			return new ExcelFileReader(workbook).read();
		}
	}

	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	private static class ExcelFileReader {
		private static OptionalInt getColumn(final Row row, final String value) {
			final int numberOfColumns = row.getLastCellNum() + 1;
			for (int column = row.getFirstCellNum(); column <= numberOfColumns; column += 1) {
				if (Strings.equalsIgnoreCaseAscii(CellValues.getAsString(CellValues.create(row.getCell(column), true)),
						value)) {
					return OptionalInt.of(column);
				}
			}
			return OptionalInt.empty();
		}

		private static Optional<String> getValue(final Row row, final int column) {
			final Cell cell = row.getCell(column);
			return cell == null
					? Optional.empty()
					: Optionals.ofNonBlank(CellValues.getAsString(CellValues.create(cell, true)));
		}

		private static Optional<String> getValue(final Row row, final OptionalInt column) {
			return OptionalInts.flatMapToObj(column, c -> getValue(row, c));
		}

		Workbook workbook;

		@PackagePrivate
		KeyCylinderPermissions read() {
			final Map<String, Key> keys = getKeys();
			final Map<Key, Set<String>> keyRoles = getKeyRoles(keys);

			final Map<String, Cylinder> cylinders = getCylinders();
			final Map<String, Set<Cylinder>> rolePermissions = getRolePermissions(cylinders);

			return getPermissions(keys.values(), keyRoles, cylinders.values(), rolePermissions);
		}

		private Map<String, Cylinder> getCylinders() {
			final Sheet sheet = workbook.getSheet("Schließzylinder");
			if (sheet == null) {
				throw new IllegalArgumentException(); // TODO
			}

			final Row header = sheet.getRow(sheet.getFirstRowNum());
			if (header == null) {
				throw new IllegalArgumentException(); // TODO
			}

			final int idColumn = getColumn(header, "ID").orElseThrow(); // TODO
			final OptionalInt nameColumn = getColumn(header, "Name");
			final OptionalInt sectionColumn = getColumn(header, "Bereich");
			final OptionalInt buildingColumn = getColumn(header, "Haus");
			final OptionalInt statusColumn = getColumn(header, "Status");

			return Workbooks.rows(sheet).skip(1).map(row -> {
				final Optional<String> id = getValue(row, idColumn);
				if (!id.isPresent()) {
					return null; // TODO: Test filtering out these rows
				}

				return new Cylinder(id.get(),
						getValue(row, nameColumn).orElse(""),
						getValue(row, sectionColumn),
						getValue(row, buildingColumn),
						getValue(row, statusColumn).map("ignorieren"::equals).orElse(false));
			}).collect(toLinkedHashMap(Cylinder::getId, Function.identity()));
		}

		private Map<String, Key> getKeys() {
			final Sheet sheet = workbook.getSheet("Transponder");
			if (sheet == null) {
				throw new IllegalArgumentException(); // TODO
			}

			final Row header = sheet.getRow(sheet.getFirstRowNum());
			if (header == null) {
				throw new IllegalArgumentException(); // TODO
			}

			final int idColumn = getColumn(header, "ID").orElseThrow(); // TODO
			final OptionalInt nameColumn = getColumn(header, "Name");
			final OptionalInt lastNameColumn = getColumn(header, "Nachname");
			final OptionalInt firstNameColumn = getColumn(header, "Vorname");
			final OptionalInt statusColumn = getColumn(header, "Status");

			return Workbooks.rows(sheet).skip(1).map(row -> {
				final Optional<String> id = getValue(row, idColumn);
				if (!id.isPresent()) {
					return null; // TODO: Test filtering out these rows
				}

				return new Key(id.get(),
						getValue(row, nameColumn),
						getValue(row, lastNameColumn),
						getValue(row, firstNameColumn),
						Optional.empty(),
						getValue(row, statusColumn).map("ignorieren"::equals).orElse(false));
			}).collect(toLinkedHashMap(Key::getId, Function.identity()));
		}

		private Map<Key, Set<String>> getKeyRoles(final Map<String, Key> keys) {
			final Sheet sheet = workbook.getSheet("Transponder-Berechtigungen");
			if (sheet == null) {
				throw new IllegalArgumentException(); // TODO
			}

			final Row header = sheet.getRow(sheet.getFirstRowNum());
			if (header == null) {
				throw new IllegalArgumentException(); // TODO
			}

			final int keyIdColumn = getColumn(header, "Transponder").orElseThrow(); // TODO
			final int roleColumn = getColumn(header, "Rolle").orElseThrow(); // TODO

			final Map<Key, Set<String>> keyRoles = new HashMap<>();
			final int numberOfRows = sheet.getLastRowNum() + 1;
			for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex < numberOfRows; rowIndex += 1) {
				final Row row = sheet.getRow(rowIndex);

				final Optional<String> keyId = getValue(row, keyIdColumn);
				if (keyId.isPresent()) {
					final Key key = Nullables.orElseThrow(keys.get(keyId.get())); // TODO
					final String role = getValue(row, roleColumn).orElseThrow(); // TODO

					keyRoles.computeIfAbsent(key, k -> new HashSet<>()).add(role);
				}
			}
			return unmodifiableMap(keyRoles);
		}

		private Map<String, Set<Cylinder>> getRolePermissions(final Map<String, Cylinder> cylinders) {
			final Sheet sheet = workbook.getSheet("Rollen-Berechtigungen");
			if (sheet == null) {
				throw new IllegalArgumentException(); // TODO
			}

			final Row header = sheet.getRow(sheet.getFirstRowNum());
			if (header == null) {
				throw new IllegalArgumentException(); // TODO
			}

			final int roleColumn = getColumn(header, "Rolle").orElseThrow(); // TODO
			final int cylinderIdColumn = getColumn(header, "Schließzylinder").orElseThrow(); // TODO

			final Map<String, Set<Cylinder>> rolePermissions = new HashMap<>();
			final int numberOfRows = sheet.getLastRowNum() + 1;
			for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex < numberOfRows; rowIndex += 1) {
				final Row row = sheet.getRow(rowIndex);

				final Optional<String> cylinderId = getValue(row, cylinderIdColumn);
				if (cylinderId.isPresent()) {
					final Cylinder cylinder = Nullables.orElseThrow(cylinders.get(cylinderId.get())); // TODO
					final String role = getValue(row, roleColumn).orElseThrow(); // TODO

					rolePermissions.computeIfAbsent(role, k -> new HashSet<>()).add(cylinder);
				}
			}
			return unmodifiableMap(rolePermissions);
		}

		private KeyCylinderPermissions getPermissions(final Collection<Key> keys,
				final Map<Key, Set<String>> keyRoles,
				final Collection<Cylinder> allCylinders,
				final Map<String, Set<Cylinder>> rolePermissions) {
			final Map<Key, Set<Cylinder>> permissions = new HashMap<>();
			for (final Key key : keys) {
				final Set<String> roles = keyRoles.get(key);
				if (roles != null) {
					// Copying all cylinders to retain the original order, then remove all not
					// permitted cylinders
					final Set<Cylinder> cylinders = new HashSet<>(allCylinders);
					cylinders.retainAll(roles.stream()
							.flatMap(role -> Nullables.orElse(rolePermissions.get(role), emptySet()).stream())
							.collect(toSet()));

					if (!cylinders.isEmpty()) {
						permissions.put(key, cylinders);
					}
				}
			}
			return new KeyCylinderPermissions(keys, allCylinders, permissions);
		}
	}
}
