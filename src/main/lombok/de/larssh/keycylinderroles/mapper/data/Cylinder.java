package de.larssh.keycylinderroles.mapper.data;

import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class Cylinder {
	@EqualsAndHashCode.Include
	@SuppressWarnings("PMD.ShortVariable")
	String id;

	String name;

	Optional<String> section;

	Optional<String> building;

	boolean ignore;

	public String getTitle() {
		final String title = getBuilding().map(building -> building + ", ").orElse("")
				+ getSection().map(section -> section + ", ").orElse("")
				+ name;
		return title.isEmpty() ? getId() : title;
	}
}
