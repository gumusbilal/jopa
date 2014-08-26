package cz.cvut.kbss.jopa.oom;

import java.lang.reflect.Field;
import java.util.Collection;

import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.metamodel.Attribute;
import cz.cvut.kbss.jopa.model.metamodel.EntityType;
import cz.cvut.kbss.ontodriver.exceptions.NotYetImplementedException;
import cz.cvut.kbss.ontodriver_new.model.Assertion;
import cz.cvut.kbss.ontodriver_new.model.Axiom;
import cz.cvut.kbss.ontodriver_new.model.Value;

abstract class FieldStrategy {

	final EntityType<?> et;
	final Attribute<?, ?> attribute;
	final Descriptor descriptor;
	final ObjectOntologyMapperImpl mapper;

	FieldStrategy(EntityType<?> et, Attribute<?, ?> att, Descriptor descriptor,
			ObjectOntologyMapperImpl mapper) {
		this.et = et;
		this.attribute = att;
		this.descriptor = descriptor;
		this.mapper = mapper;
	}

	/**
	 * Sets the specified value on the specified instance, the field is taken
	 * from the attribute represented by this strategy. </p>
	 * 
	 * Note that this method assumes the value and the field are of compatible
	 * types, no check is done here.
	 */
	void setValueOnInstance(Object instance, Object value) throws IllegalArgumentException,
			IllegalAccessException {
		final Field f = attribute.getJavaField();
		f.setAccessible(true);
		f.set(instance, value);
	}

	/**
	 * Adds value from the specified axioms to this strategy. </p>
	 * 
	 * The value(s) is/are then set on entity field using
	 * {@link #buildInstanceFieldValue(Object)}.
	 * 
	 * @param ax
	 *            Axiom to extract value from
	 */
	abstract void addValueFromAxiom(Axiom ax);

	/**
	 * Sets instance field from values gathered in this strategy.
	 * 
	 * @param instance
	 *            The instance to receive the field value
	 * @throws IllegalArgumentException
	 *             Access error
	 * @throws IllegalAccessException
	 *             Access error
	 */
	abstract void buildInstanceFieldValue(Object instance) throws IllegalArgumentException,
			IllegalAccessException;

	/**
	 * Extracts values of field represented by this strategy from the specified
	 * instance.
	 * 
	 * @param instance
	 *            The instance to extract values from
	 * @return Collection of values, empty collection if there are none
	 * @throws IllegalArgumentException
	 *             Access error
	 * @throws IllegalAccessException
	 *             Access error
	 */
	abstract Collection<Value<?>> extractAttributeValuesFromInstance(Object instance)
			throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Creates property assertion appropriate for the attribute represented by
	 * this strategy.
	 * 
	 * @return Property assertion
	 */
	abstract Assertion createAssertion();

	static FieldStrategy createFieldStrategy(EntityType<?> et, Attribute<?, ?> att,
			Descriptor descriptor, ObjectOntologyMapperImpl mapper) {
		if (att.isCollection()) {
			switch (att.getPersistentAttributeType()) {
			case ANNOTATION:
			case DATA:
				throw new NotYetImplementedException();
			case OBJECT:
				return new PluralObjectPropertyStrategy(et, att, descriptor, mapper);
			default:
				break;
			}
		} else {
			switch (att.getPersistentAttributeType()) {
			case ANNOTATION:
			case DATA:
				return new SingularDataPropertyStrategy(et, att, descriptor, mapper);
			case OBJECT:
				return new SingularObjectPropertyStrategy(et, att, descriptor, mapper);
			default:
				break;
			}
		}
		// Shouldn't happen
		throw new IllegalArgumentException();
	}
}
