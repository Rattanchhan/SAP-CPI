package com.model.iservice.base;

import java.sql.Timestamp;
import java.util.List;

public interface IPersistent {
	/**
	 * The object id is not intended to be used in any application context but to
	 * identify objects in the data store and to determine object identity in the
	 * equals() method.
	 * 
	 * @return the objects ID
	 */
	public abstract long getId();

	/**
	 * Mark the object as deleted if isDeleteable() returns true.
	 */
	public abstract void markDeleted();

	/**
	 * @return deleted attribute;
	 */
	public abstract boolean isMarkedDeleted();

	/**
	 * Set markCanged if it's not marked new.
	 */
	public abstract void markChanged();
		
	/**
	 * @return a List of attribute changes made since instantiation or reset(). 
	 */
	public List<?> listModifications();
	
	/**
	 * @return the changed flag.
	 */
	
	public abstract boolean isMarkedChanged();

	/**
	 * @return the the new flag.
	 */
	public abstract boolean isNew();

	/**
	 * Reset all state indicators (deleted, changed, new) to false.
	 */
	public abstract void reset();

	/**
	 * This method returns the natural key of this object. This method has to be
	 * implemented by subclasses
	 * 
	 * @return the key as Object
	 */
	public abstract Object getKey();

	/**
	 * mark as new if not changed or deleted.
	 * 
	 * @throws IllegalStateException
	 *           if id marked new or deleted.
	 */
	public abstract void markNew();

	/**
	 * This method has to be overwritten by subclasses to indicate whether its
	 * state allows deletion. This default implementation returns always true.
	 * 
	 * @return true
	 */
	public abstract boolean isDeleteable();

	/**
	 * @return a string containing the class name (without package) and the result
	 *         of getKey(). E.g. "Tank { Tank1 }"
	 */
	public abstract String toString();

	/**
	 * @return the objects key hashcode calculated from the getKey() result.
	 * @see java.lang.Object#hashCode()
	 */
	public abstract int hashCode();

	/**
	 * This equals method uses the getKey() results to compare the two objects.
	 * Checks if the objects are identical. Checks if the classes are the same.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public abstract boolean equals(Object other);

	/**
	 * @return the timestamp when this object has been created.
	 */
	public Timestamp getCreatedAt();

	/**
	 * @return the timestamp when this object has been modified
	 */
	public Timestamp getModifiedAt();

	/**
	 * 
	 * @return who has modified this object
	 */
	public String getModifiedBy();

	/**
	 * Set the (user) Id of the modifier
	 * 
	 * @param modifiedBy
	 */
	public void setModifiedBy(String modifiedBy);

	/**
	 * @return the internal version number used for optimistic locking
	 */
	public long getVersionNr();

	/**
	 * @return the generated object UID (not the database id for this object)
	 */
	public long getObjectUId();

	/**
	 * @return the length of string fields
	 */
	public int getPropertyLength( String propertyName );

}
