package com.model.iservice.base;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;





public abstract class PersistentDTO implements IPersistent {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8833570923472145605L;
	public static final String ATTRIBUTE_CREATED_AT = "createdAt";
	public static final String ATTRIBUTE_MODIFIED_AT = "modifiedAt";
	public static final String ATTRIBUTE_MODIFIED_BY = "modifiedBy";
	public static final String ATTRIBUTE_ID = "id";
	public static final String ATTRIBUTE_VERSION_NR = "versionNr";
	public static final String ATTRIBUTE_OJECT_UID = "objectUId";
	
	/*
	 * Fields which are not part of the persistent state of the object
	 */
	private transient boolean markedDeleted = false;
	private transient boolean markedChanged = false;
	private transient boolean isNew = false;
	private transient List modifications = new ArrayList(1);
	/**
	 * Fields which are part of the persistent state of the object
	 */
	private long id;
	private Timestamp createdAt = new Timestamp(System.currentTimeMillis());
	private Timestamp modifiedAt;
	private String modifiedBy;
	private long versionNr;
	private long objectUId;
	private Hashtable attributeLengths = null; // Hashtable of the string attributes length

	protected PersistentDTO() {
		super();
		objectUId = UIDGenerator.getInstance().getUID();
		
	}
	
	
	/**
	 * @return the length of the String properties from the hibernate configuration
	 */
	public int getPropertyLength(String propertyName) {
		if( attributeLengths == null )
			return -1;
		Integer length = (Integer) attributeLengths.get( propertyName );
		if( length == null )
			return -1;
		return length.intValue();
	}
	
	

	/**
	 * @hibernate.id generator-class="native" column="oid"
	 */
	public long getId() {
		return id;
	}

	/**
	 * @hibernate.version type="long" column="version_nr" access="field"
	 * @see com.endress.infoserve.persistence.model.IPersistable#getVersionNr()
	 */
	public long getVersionNr() {
		return versionNr;
	}

	/**
	 * @hibernate.property access="field" column="created_at"
	 * @see com.endress.infoserve.persistence.model.IPersistable#getCreatedAt()
	 */
	public Timestamp getCreatedAt() {
		return createdAt;
	}

	/**
	 * @hibernate.property optimistic-lock="false" access="field" column="modified_by"
	 * @see com.endress.infoserve.persistence.model.IPersistable#getModifiedBy()
	 */
	public String getModifiedBy() {
		return modifiedBy;
	}

	/**
	 * @hibernate.property optimistic-lock="false" access="field" column="modified_at"
	 * @see com.endress.infoserve.persistence.model.IPersistable#getModifiedAt()
	 */
	public Timestamp getModifiedAt() {
		return modifiedAt;
	}

	// ------------------------------------------------------------------------------
	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#setId(long)
	 */
	protected void setId(long id) {
		this.id = id;
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#markDeleted()
	 */
	public void markDeleted() {
		if (isDeleteable()) {
			markedChanged = false;
			markedDeleted = true;
		}
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#isMarkedDeleted()
	 */
	public boolean isMarkedDeleted() {
		return markedDeleted;
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#markChanged()
	 */
	public void markChanged() {
		if (!isNew && !isMarkedDeleted()) {
			markedChanged = true;
			modifiedAt = new Timestamp(System.currentTimeMillis());
		}
	}

	public List listModifications() {
		return modifications;
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#isMarkedChanged()
	 */
	public boolean isMarkedChanged() {
		return markedChanged;
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#isNew()
	 */
	public boolean isNew() {
		return isNew;
	}

	public void markChangedRelationship(PersistentDTO partner) {
		markChanged();
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#reset()
	 */
	public void reset() {
		markedDeleted = false;
		markedChanged = false;
		isNew = false;
		synchronized (this) {
			modifications.clear();
		}
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#markNew()
	 */
	public void markNew() {
		isNew = true;
		/*
		 * PZ : dont'know who has changed this coding in a way that it is not
		 * conditional anymore if (!isMarkedChanged() && !isMarkedDeleted()) isNew =
		 * true; else isNew = true; // throw new IllegalStateException("Object must
		 * not be // 'deleted' or 'changed'");
		 * 
		 */
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#isDeleteable()
	 */
	public boolean isDeleteable() {
		return true;
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#toString()
	 */
	public String toString() {
		String result = getClass().getName();
		result = result.substring(result.lastIndexOf('.') + 1);
		return result + "{ " + getKey() + " }";
	}

	/**
	 * This method returns the natural key of this object.
	 * 
	 * @return the key as Object
	 */
	public Object getKey() {
		return new Long(getObjectUId());
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#hashCode()
	 */
	public int hashCode() {
		return (int) getObjectUId();
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if ( other == null )
			return false;
		
		Class thisClass = this.getClass();
		Class otherClass = other.getClass();
		if (!thisClass.isAssignableFrom(otherClass) && !(otherClass.isAssignableFrom(thisClass))) {
			return false;
		}
		if (!(other instanceof PersistentDTO)) {
			return false;
		}
		PersistentDTO theOther = (PersistentDTO) other;
		boolean result = (getObjectUId() == theOther.getObjectUId());
		return result;
	}

	/**
	 * @see com.endress.infoserve.persistence.model.IPersistable#setModifiedBy(java.lang.String)
	 */
	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public void setVersionNr(long newVersionNr) {
		versionNr = newVersionNr;
	}

	/**
	 * @hibernate.property access="field" column="objectuid"
	 * @return the objectUId
	 */
	public long getObjectUId() {
		return objectUId;
	}

	public void setObjectUId(long newuid) {
		this.objectUId = newuid;
	}

}
