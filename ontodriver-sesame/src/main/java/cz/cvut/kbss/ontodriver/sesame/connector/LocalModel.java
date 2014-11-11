package cz.cvut.kbss.ontodriver.sesame.connector;

import java.util.Collection;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;

/**
 * Caches local transactional changes to the Sesame repository model.
 * 
 * @author ledvima1
 * 
 */
class LocalModel {

	private final Model addedStatements;
	private final Model removedStatements;

	LocalModel() {
		this.addedStatements = new LinkedHashModel();
		this.removedStatements = new LinkedHashModel();
	}

	void enhanceStatements(Collection<Statement> statements, Resource subject, URI property,
			Value object, URI... contexts) {
		final URI[] ctxs = contexts != null ? contexts : new URI[0];
		final Collection<Statement> added = addedStatements.filter(subject, property, object, ctxs);
		statements.addAll(added);
		final Collection<Statement> removed = removedStatements.filter(subject, property, object,
				ctxs);
		statements.removeAll(removed);
	}

	void addStatements(Collection<Statement> statements) {
		removedStatements.removeAll(statements);
		addedStatements.addAll(statements);
	}

	void removeStatements(Collection<Statement> statements) {
		addedStatements.removeAll(statements);
		removedStatements.addAll(statements);
	}

	Collection<Statement> getAddedStatements() {
		return addedStatements;
	}

	Collection<Statement> getRemovedStatements() {
		return removedStatements;
	}
}