package include.KVStoreSimulator; /**
 * Autogenerated by Thrift Compiler (0.9.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */

import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

/**
 * Enum type for returning status of a RPC request made to the
 * the storage server.
 */
public enum KVStoreStatus implements org.apache.thrift.TEnum {
  OK(1),
  EKEYNOTFOUND(2),
  EITEMNOTFOUND(3),
  EPUTFAILED(4),
  EITEMEXISTS(5),
  INTERNAL_FAILURE(6),
  NOT_IMPLEMENTED(7);

  private final int value;

  private KVStoreStatus(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static KVStoreStatus findByValue(int value) { 
    switch (value) {
      case 1:
        return OK;
      case 2:
        return EKEYNOTFOUND;
      case 3:
        return EITEMNOTFOUND;
      case 4:
        return EPUTFAILED;
      case 5:
        return EITEMEXISTS;
      case 6:
        return INTERNAL_FAILURE;
      case 7:
        return NOT_IMPLEMENTED;
      default:
        return null;
    }
  }
}
