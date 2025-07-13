/**
 * IIntegration.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package com.sforce.soap.schemas._class.IServices;


public class IIntegration  implements java.io.Serializable {
    
	private static final long serialVersionUID = -981054725471389936L;

	private java.lang.String fromSystem;
    private java.lang.String mappingName;

    private com.sforce.soap.schemas._class.IServices.IBean[][] records;
    private java.lang.String targetObject;

    public IIntegration() {
    }

    public IIntegration(
           java.lang.String fromSystem,
           java.lang.String mappingName,
           com.sforce.soap.schemas._class.IServices.IBean[][] records,
           java.lang.String targetObject) {
           this.fromSystem = fromSystem;
           this.mappingName = mappingName;
           this.records = records;
           this.targetObject = targetObject;
    }

    /**
     * Gets the fromSystem value for this IIntegration.
     * 
     * @return fromSystem
     */
    public java.lang.String getFromSystem() {
        return fromSystem;
    }


    /**
     * Sets the fromSystem value for this IIntegration.
     * 
     * @param fromSystem
     */
    public void setFromSystem(java.lang.String fromSystem) {
        this.fromSystem = fromSystem;
    }


    /**
     * Gets the mappingName value for this IIntegration.
     * 
     * @return mappingName
     */
    public java.lang.String getMappingName() {
        return mappingName;
    }


    /**
     * Sets the mappingName value for this IIntegration.
     * 
     * @param mappingName
     */
    public void setMappingName(java.lang.String mappingName) {
        this.mappingName = mappingName;
    }


    /**
     * Gets the records value for this IIntegration.
     * 
     * @return records
     */
    public com.sforce.soap.schemas._class.IServices.IBean[][] getRecords() {
        return records;
    }


    /**
     * Sets the records value for this IIntegration.
     * 
     * @param records
     */
    public void setRecords(com.sforce.soap.schemas._class.IServices.IBean[][] records) {
        this.records = records;
    }

    public com.sforce.soap.schemas._class.IServices.IBean[] getRecords(int i) {
        return this.records[i];
    }

    public void setRecords(int i, com.sforce.soap.schemas._class.IServices.IBean[] _value) {
        this.records[i] = _value;
    }


    /**
     * Gets the targetObject value for this IIntegration.
     * 
     * @return targetObject
     */
    public java.lang.String getTargetObject() {
        return targetObject;
    }


    /**
     * Sets the targetObject value for this IIntegration.
     * 
     * @param targetObject
     */
    public void setTargetObject(java.lang.String targetObject) {
        this.targetObject = targetObject;
    }

    private java.lang.Object __equalsCalc = null;
    @Override
	public synchronized boolean equals(java.lang.Object obj) {
    	if (obj == null) return false;
        if (!(obj instanceof IIntegration)) return false;
        IIntegration other = (IIntegration) obj;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.fromSystem==null && other.getFromSystem()==null) || 
             (this.fromSystem!=null &&
              this.fromSystem.equals(other.getFromSystem()))) &&
            ((this.mappingName==null && other.getMappingName()==null) || 
             (this.mappingName!=null &&
              this.mappingName.equals(other.getMappingName()))) &&
            ((this.records==null && other.getRecords()==null) || 
             (this.records!=null &&
              java.util.Arrays.equals(this.records, other.getRecords()))) &&
            ((this.targetObject==null && other.getTargetObject()==null) || 
             (this.targetObject!=null &&
              this.targetObject.equals(other.getTargetObject())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    @Override
	public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getFromSystem() != null) {
            _hashCode += getFromSystem().hashCode();
        }
        if (getMappingName() != null) {
            _hashCode += getMappingName().hashCode();
        }
        if (getRecords() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getRecords());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getRecords(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getTargetObject() != null) {
            _hashCode += getTargetObject().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

   
	
}
