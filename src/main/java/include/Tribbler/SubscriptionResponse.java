package include.Tribbler; /**
 * Autogenerated by Thrift Compiler (0.9.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Return type for a RPC call to the Tribbler server request user's list of
 * subscriptions.
 */
public class SubscriptionResponse implements org.apache.thrift.TBase<SubscriptionResponse, SubscriptionResponse._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("SubscriptionResponse");

  private static final org.apache.thrift.protocol.TField SUBSCRIPTIONS_FIELD_DESC = new org.apache.thrift.protocol.TField("subscriptions", org.apache.thrift.protocol.TType.LIST, (short)1);
  private static final org.apache.thrift.protocol.TField STATUS_FIELD_DESC = new org.apache.thrift.protocol.TField("status", org.apache.thrift.protocol.TType.I32, (short)2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new SubscriptionResponseStandardSchemeFactory());
    schemes.put(TupleScheme.class, new SubscriptionResponseTupleSchemeFactory());
  }

  public List<String> subscriptions; // required
  /**
   * 
   * @see TribbleStatus
   */
  public TribbleStatus status; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    SUBSCRIPTIONS((short)1, "subscriptions"),
    /**
     * 
     * @see TribbleStatus
     */
    STATUS((short)2, "status");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // SUBSCRIPTIONS
          return SUBSCRIPTIONS;
        case 2: // STATUS
          return STATUS;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.SUBSCRIPTIONS, new org.apache.thrift.meta_data.FieldMetaData("subscriptions", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.STATUS, new org.apache.thrift.meta_data.FieldMetaData("status", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, TribbleStatus.class)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(SubscriptionResponse.class, metaDataMap);
  }

  public SubscriptionResponse() {
  }

  public SubscriptionResponse(
    List<String> subscriptions,
    TribbleStatus status)
  {
    this();
    this.subscriptions = subscriptions;
    this.status = status;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public SubscriptionResponse(SubscriptionResponse other) {
    if (other.isSetSubscriptions()) {
      List<String> __this__subscriptions = new ArrayList<String>();
      for (String other_element : other.subscriptions) {
        __this__subscriptions.add(other_element);
      }
      this.subscriptions = __this__subscriptions;
    }
    if (other.isSetStatus()) {
      this.status = other.status;
    }
  }

  public SubscriptionResponse deepCopy() {
    return new SubscriptionResponse(this);
  }

  @Override
  public void clear() {
    this.subscriptions = null;
    this.status = null;
  }

  public int getSubscriptionsSize() {
    return (this.subscriptions == null) ? 0 : this.subscriptions.size();
  }

  public java.util.Iterator<String> getSubscriptionsIterator() {
    return (this.subscriptions == null) ? null : this.subscriptions.iterator();
  }

  public void addToSubscriptions(String elem) {
    if (this.subscriptions == null) {
      this.subscriptions = new ArrayList<String>();
    }
    this.subscriptions.add(elem);
  }

  public List<String> getSubscriptions() {
    return this.subscriptions;
  }

  public SubscriptionResponse setSubscriptions(List<String> subscriptions) {
    this.subscriptions = subscriptions;
    return this;
  }

  public void unsetSubscriptions() {
    this.subscriptions = null;
  }

  /** Returns true if field subscriptions is set (has been assigned a value) and false otherwise */
  public boolean isSetSubscriptions() {
    return this.subscriptions != null;
  }

  public void setSubscriptionsIsSet(boolean value) {
    if (!value) {
      this.subscriptions = null;
    }
  }

  /**
   * 
   * @see TribbleStatus
   */
  public TribbleStatus getStatus() {
    return this.status;
  }

  /**
   * 
   * @see TribbleStatus
   */
  public SubscriptionResponse setStatus(TribbleStatus status) {
    this.status = status;
    return this;
  }

  public void unsetStatus() {
    this.status = null;
  }

  /** Returns true if field status is set (has been assigned a value) and false otherwise */
  public boolean isSetStatus() {
    return this.status != null;
  }

  public void setStatusIsSet(boolean value) {
    if (!value) {
      this.status = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case SUBSCRIPTIONS:
      if (value == null) {
        unsetSubscriptions();
      } else {
        setSubscriptions((List<String>)value);
      }
      break;

    case STATUS:
      if (value == null) {
        unsetStatus();
      } else {
        setStatus((TribbleStatus)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case SUBSCRIPTIONS:
      return getSubscriptions();

    case STATUS:
      return getStatus();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case SUBSCRIPTIONS:
      return isSetSubscriptions();
    case STATUS:
      return isSetStatus();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof SubscriptionResponse)
      return this.equals((SubscriptionResponse)that);
    return false;
  }

  public boolean equals(SubscriptionResponse that) {
    if (that == null)
      return false;

    boolean this_present_subscriptions = true && this.isSetSubscriptions();
    boolean that_present_subscriptions = true && that.isSetSubscriptions();
    if (this_present_subscriptions || that_present_subscriptions) {
      if (!(this_present_subscriptions && that_present_subscriptions))
        return false;
      if (!this.subscriptions.equals(that.subscriptions))
        return false;
    }

    boolean this_present_status = true && this.isSetStatus();
    boolean that_present_status = true && that.isSetStatus();
    if (this_present_status || that_present_status) {
      if (!(this_present_status && that_present_status))
        return false;
      if (!this.status.equals(that.status))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(SubscriptionResponse other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    SubscriptionResponse typedOther = (SubscriptionResponse)other;

    lastComparison = Boolean.valueOf(isSetSubscriptions()).compareTo(typedOther.isSetSubscriptions());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSubscriptions()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.subscriptions, typedOther.subscriptions);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetStatus()).compareTo(typedOther.isSetStatus());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStatus()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.status, typedOther.status);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("SubscriptionResponse(");
    boolean first = true;

    sb.append("subscriptions:");
    if (this.subscriptions == null) {
      sb.append("null");
    } else {
      sb.append(this.subscriptions);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("status:");
    if (this.status == null) {
      sb.append("null");
    } else {
      sb.append(this.status);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class SubscriptionResponseStandardSchemeFactory implements SchemeFactory {
    public SubscriptionResponseStandardScheme getScheme() {
      return new SubscriptionResponseStandardScheme();
    }
  }

  private static class SubscriptionResponseStandardScheme extends StandardScheme<SubscriptionResponse> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, SubscriptionResponse struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // SUBSCRIPTIONS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list8 = iprot.readListBegin();
                struct.subscriptions = new ArrayList<String>(_list8.size);
                for (int _i9 = 0; _i9 < _list8.size; ++_i9)
                {
                  String _elem10; // required
                  _elem10 = iprot.readString();
                  struct.subscriptions.add(_elem10);
                }
                iprot.readListEnd();
              }
              struct.setSubscriptionsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // STATUS
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.status = TribbleStatus.findByValue(iprot.readI32());
              struct.setStatusIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, SubscriptionResponse struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.subscriptions != null) {
        oprot.writeFieldBegin(SUBSCRIPTIONS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.subscriptions.size()));
          for (String _iter11 : struct.subscriptions)
          {
            oprot.writeString(_iter11);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.status != null) {
        oprot.writeFieldBegin(STATUS_FIELD_DESC);
        oprot.writeI32(struct.status.getValue());
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class SubscriptionResponseTupleSchemeFactory implements SchemeFactory {
    public SubscriptionResponseTupleScheme getScheme() {
      return new SubscriptionResponseTupleScheme();
    }
  }

  private static class SubscriptionResponseTupleScheme extends TupleScheme<SubscriptionResponse> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, SubscriptionResponse struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetSubscriptions()) {
        optionals.set(0);
      }
      if (struct.isSetStatus()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetSubscriptions()) {
        {
          oprot.writeI32(struct.subscriptions.size());
          for (String _iter12 : struct.subscriptions)
          {
            oprot.writeString(_iter12);
          }
        }
      }
      if (struct.isSetStatus()) {
        oprot.writeI32(struct.status.getValue());
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, SubscriptionResponse struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list13 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.subscriptions = new ArrayList<String>(_list13.size);
          for (int _i14 = 0; _i14 < _list13.size; ++_i14)
          {
            String _elem15; // required
            _elem15 = iprot.readString();
            struct.subscriptions.add(_elem15);
          }
        }
        struct.setSubscriptionsIsSet(true);
      }
      if (incoming.get(1)) {
        struct.status = TribbleStatus.findByValue(iprot.readI32());
        struct.setStatusIsSet(true);
      }
    }
  }

}

