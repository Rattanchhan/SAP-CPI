/**
 * CallOptions.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.sforce.soap.schemas._class.IServices;

public class CallOptions  implements java.io.Serializable {
    private java.lang.String client;

    private java.lang.String defaultNamespace;

    public CallOptions() {
    }

    public CallOptions(
           java.lang.String client,
           java.lang.String defaultNamespace) {
           this.client = client;
           this.defaultNamespace = defaultNamespace;
    }


    /**
     * Gets the client value for this CallOptions.
     * 
     * @return client
     */
    public java.lang.String getClient() {
        return client;
    }


    /**
     * Sets the client value for this CallOptions.
     * 
     * @param client
     */
    public void setClient(java.lang.String client) {
        this.client = client;
    }


    /**
     * Gets the defaultNamespace value for this CallOptions.
     * 
     * @return defaultNamespace
     */
    public java.lang.String getDefaultNamespace() {
        return defaultNamespace;
    }


    /**
     * Sets the defaultNamespace value for this CallOptions.
     * 
     * @param defaultNamespace
     */
    public void setDefaultNamespace(java.lang.String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    private java.lang.Object __equalsCalc = null;
    @Override
	public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof CallOptions)) return false;
        CallOptions other = (CallOptions) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.client==null && other.getClient()==null) || 
             (this.client!=null &&
              this.client.equals(other.getClient()))) &&
            ((this.defaultNamespace==null && other.getDefaultNamespace()==null) || 
             (this.defaultNamespace!=null &&
              this.defaultNamespace.equals(other.getDefaultNamespace())));
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
        if (getClient() != null) {
            _hashCode += getClient().hashCode();
        }
        if (getDefaultNamespace() != null) {
            _hashCode += getDefaultNamespace().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    

}
