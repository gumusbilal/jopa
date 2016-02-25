/**
 * Copyright (C) 2016 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.model.metamodel;

/**
 * Instances of the type PropertiesSpecification represent persistent properties
 * of other types than declared in the entity type.
 * 
 * @param <X>
 *            The type the represented Set belongs to
 * @param <E>
 *            The element type of the represented Set
 */
public interface PropertiesSpecification<X, E> extends FieldSpecification<X, E> {
}
