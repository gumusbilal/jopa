package cz.cvut.kbss.jopa.oom;

import java.lang.reflect.Field;
import java.net.URI;

import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.metamodel.Attribute;
import cz.cvut.kbss.jopa.model.metamodel.EntityType;
import cz.cvut.kbss.jopa.model.metamodel.FieldSpecification;
import cz.cvut.kbss.jopa.model.metamodel.ListAttribute;
import cz.cvut.kbss.jopa.model.metamodel.PluralAttribute;
import cz.cvut.kbss.jopa.model.metamodel.PropertiesSpecification;
import cz.cvut.kbss.jopa.model.metamodel.TypesSpecification;
import cz.cvut.kbss.jopa.utils.EntityPropertiesUtils;
import cz.cvut.kbss.ontodriver.exceptions.NotYetImplementedException;
import cz.cvut.kbss.ontodriver_new.model.Assertion;
import cz.cvut.kbss.ontodriver_new.model.Axiom;

abstract class FieldStrategy<T extends FieldSpecification<? super X, ?>, X> {

	final EntityType<X> et;
	final T attribute;
	final Descriptor descriptor;
	final EntityMappingHelper mapper;
	CascadeResolver cascadeResolver;

	FieldStrategy(EntityType<X> et, T att, Descriptor descriptor,
			EntityMappingHelper mapper) {
		this.et = et;
		this.attribute = att;
		this.descriptor = descriptor;
		this.mapper = mapper;
	}

	void setCascadeResolver(CascadeResolver resolver) {
		this.cascadeResolver = resolver;
	}

	/**
	 * Sets the specified value on the specified instance, the field is taken
	 * from the attribute represented by this strategy. </p>
	 * 
	 * Note that this method assumes the value and the field are of compatible
	 * types, no check is done here.
	 */
	void setValueOnInstance(Object instance, Object value)
			throws IllegalArgumentException, IllegalAccessException {
		final Field field = attribute.getJavaField();
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		field.set(instance, value);
	}

	/**
	 * Extracts the attribute value from the specified instance. </p>
	 * 
	 * @return Attribute value, possibly {@code null}
	 */
	Object extractFieldValueFromInstance(Object instance)
			throws IllegalArgumentException, IllegalAccessException {
		final Field field = attribute.getJavaField();
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		return field.get(instance);
	}

	<E> URI resolveValueIdentifier(E instance, EntityType<E> valEt) {
		URI id = EntityPropertiesUtils.getPrimaryKey(instance, valEt);
		if (id == null) {
			id = mapper.generateIdentifier(valEt);
			EntityPropertiesUtils.setPrimaryKey(id, instance, valEt);
		}
		return id;
	}

	URI getAttributeContext() {
		return descriptor.getAttributeDescriptor(attribute).getContext();
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
	abstract void addValueFromAxiom(Axiom<?> ax);

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
	abstract void buildInstanceFieldValue(Object instance)
			throws IllegalArgumentException, IllegalAccessException;

	/**
	 * Extracts values of field represented by this strategy from the specified
	 * instance.
	 * 
	 * @param instance
	 *            The instance to extract values from
	 * @param valueBuilder
	 *            Builder into which the attribute value(s) are extracted
	 * @throws IllegalArgumentException
	 *             Access error
	 * @throws IllegalAccessException
	 *             Access error
	 */
	abstract void buildAxiomValuesFromInstance(X instance,
			AxiomValueGatherer valueBuilder) throws IllegalArgumentException,
			IllegalAccessException;

	/**
	 * Creates property assertion appropriate for the attribute represented by
	 * this strategy.
	 * 
	 * @return Property assertion
	 */
	abstract Assertion createAssertion();

	static <X> FieldStrategy<? extends FieldSpecification<? super X, ?>, X> createFieldStrategy(
			EntityType<X> et, FieldSpecification<? super X, ?> att,
			Descriptor descriptor, EntityMappingHelper mapper) {
		if (att instanceof TypesSpecification) {
			return new TypesFieldStrategy<>(et,
					(TypesSpecification<? super X, ?>) att, descriptor, mapper);
		} else if (att instanceof PropertiesSpecification<?, ?>) {
			return new PropertiesFieldStrategy<>(et,
					(PropertiesSpecification<? super X, ?>) att, descriptor,
					mapper);
		}
		final Attribute<? super X, ?> attribute = (Attribute<? super X, ?>) att;
		if (attribute.isCollection()) {
			switch (attribute.getPersistentAttributeType()) {
			case ANNOTATION:
			case DATA:
				throw new NotYetImplementedException();
			case OBJECT:
				return createPluralObjectPropertyStrategy(et,
						(PluralAttribute<? super X, ?, ?>) attribute,
						descriptor, mapper);
			default:
				break;
			}
		} else {
			switch (attribute.getPersistentAttributeType()) {
			case ANNOTATION:
			case DATA:
				return new SingularDataPropertyStrategy<>(et, attribute,
						descriptor, mapper);
			case OBJECT:
				return new SingularObjectPropertyStrategy<>(et, attribute,
						descriptor, mapper);
			default:
				break;
			}
		}
		// Shouldn't happen
		throw new IllegalArgumentException();
	}

	private static <Y> FieldStrategy<? extends FieldSpecification<? super Y, ?>, Y> createPluralObjectPropertyStrategy(
			EntityType<Y> et, PluralAttribute<? super Y, ?, ?> attribute,
			Descriptor descriptor, EntityMappingHelper mapper) {
		switch (attribute.getCollectionType()) {
		case LIST:
			final ListAttribute<? super Y, ?> listAtt = (ListAttribute<? super Y, ?>) attribute;
			switch (listAtt.getSequenceType()) {
			case referenced:
				return new ReferencedListPropertyStrategy<>(et, listAtt,
						descriptor, mapper);
			case simple:
				return new SimpleListPropertyStrategy<>(et, listAtt,
						descriptor, mapper);
			default:
				throw new NotYetImplementedException(
						"Unsupported list attribute sequence type "
								+ listAtt.getSequenceType());
			}
		case SET:
			return new SimpleSetPropertyStrategy<>(et, attribute, descriptor,
					mapper);
		default:
			throw new NotYetImplementedException(
					"Unsupported plural attribute collection type "
							+ attribute.getCollectionType());
		}
	}
}