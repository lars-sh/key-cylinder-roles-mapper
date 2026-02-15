package de.larssh.keycylinderroles.mapper.data;

import static de.larssh.utils.Collectors.toLinkedHashMap;
import static de.larssh.utils.Collectors.toMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import de.larssh.utils.collection.Maps;
import lombok.ToString;

@ToString
public class KeyCylinderPermissions {
	Map<Key, Key> keys;

	Map<Cylinder, Cylinder> cylinders;

	Map<Key, Set<Cylinder>> permissions;

	public KeyCylinderPermissions(final Collection<Key> keys,
			final Collection<Cylinder> cylinders,
			final Map<Key, Set<Cylinder>> permissions) {
		this.keys = keys.stream().collect(toLinkedHashMap(Function.identity(), Function.identity()));
		this.cylinders = cylinders.stream().collect(toLinkedHashMap(Function.identity(), Function.identity()));
		this.permissions = permissions.entrySet()
				.stream()
				.map(entry -> Maps.entry(entry.getKey(), new HashSet<>(entry.getValue())))
				.collect(toMap(Entry::getKey, Entry::getValue));
	}

	public boolean allows(final Key key, final Cylinder cylinder) {
		final Set<Cylinder> allowedCylinders = permissions.get(key);
		return allowedCylinders != null && allowedCylinders.contains(cylinder);
	}

	public Optional<Cylinder> get(final Cylinder cylinder) {
		return Optional.ofNullable(cylinders.get(cylinder));
	}

	public Optional<Key> get(final Key key) {
		return Optional.ofNullable(keys.get(key));
	}

	public Set<Key> getKeys() {
		return keys.keySet();
	}

	public Set<Cylinder> getCylinders() {
		return cylinders.keySet();
	}

	public boolean isIgnore(final Cylinder cylinder) {
		return get(cylinder).map(Cylinder::isIgnore).orElse(Boolean.FALSE);
	}

	public boolean isIgnore(final Key key) {
		return get(key).map(Key::isIgnore).orElse(Boolean.FALSE);
	}
}
