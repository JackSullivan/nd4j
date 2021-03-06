/*
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 *
 */
package jcuda.driver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * <u>Note:</u> This class should be considered as preliminary,
 * and might change in future releases. <br />
 * <br /> 
 * A utility class to circumvent the limitations of Java in terms
 * of interpreting memory areas as pointers or primitive values:
 * <br />
 * <br />
 * This class allows mapping {@link CUjit_option} identifiers to
 * their respective values, so that the options may be passed 
 * to methods that require the parameters <br /> 
 * <code>unsigned int numOptions, CUjit_option *options, 
 * void **optionValues</code><br />
 * in the original CUDA API:
 */
public final class JITOptions
{
    /**
     * The map storing the options and their values
     */
    private final Map<Integer, Object> map = 
        new LinkedHashMap<Integer, Object>();
    
    /**
     * Creates new, empty JITOptions
     */
    public JITOptions()
    {
    }
    
    /**
     * Package private method to obtain the keys stored in these
     * options. Also used on native side.
     * 
     * @return The keys of these options
     */
    int[] getKeys()
    {
        Set<Integer> keySet = map.keySet();
        int keys[] = new int[keySet.size()];
        int index = 0;
        for (Integer key : keySet)
        {
            keys[index] = key;
            index++;
        }
        return keys;
    }
    
    /**
     * Removes the specified option
     * 
     * @param key An option identifier
     */
    public void remove(int key)
    {
        map.remove(key);
    }

    /**
     * Put the specified option into these options (without a value)
     * 
     * @param key An option identifier
     */
    public void put(int key)
    {
        map.put(key, null);
    }
    
    /**
     * Put the given value for the specified option into these options 
     * 
     * @param key An option identifier
     * @param value The option value
     */
    public void putInt(int key, int value)
    {
        map.put(key, value);
    }

    /**
     * Put the given value for the specified option into these options 
     * 
     * @param key An option identifier
     * @param value The option value
     */
    public void putFloat(int key, float value)
    {
        map.put(key, value);
    }

    /**
     * Put the given value for the specified option into these options 
     * 
     * @param key An option identifier
     * @param value The option value
     */
    public void putBytes(int key, byte value[])
    {
        map.put(key, value);
    }
    
    /**
     * Returns the value of the given option. Returns 0 if the specified
     * option is unknown or not an <code>int</code> or 
     * <code>unsigned int</code> value. 
     * 
     * @param key An option identifier
     * @return The option value
     */
    public int getInt(int key)
    {
        Object value = map.get(key);
        if (!(value instanceof Integer))
        {
            return 0;
        }
        Integer result = (Integer)value;
        return result;
    }
    
    /**
     * Returns the value of the given option. Returns 0 if the specified
     * option is unknown or not a <code>float</code> value. 
     * 
     * @param key An option identifier
     * @return The option value
     */
    public float getFloat(int key)
    {
        Object value = map.get(key);
        if (!(value instanceof Float))
        {
            return 0.0f;
        }
        Float result = (Float)value;
        return result;
    }
    
    /**
     * Returns the value of the given option. Returns <code>null</code> if 
     * the specified option is unknown or not a <code>byte[]</code> value. 
     * 
     * @param key An option identifier
     * @return The option value
     */
    public byte[] getBytes(int key)
    {
        Object value = map.get(key);
        if (value == null)
        {
            return null;
        }
        if (!(value instanceof byte[]))
        {
            return null;
        }
        byte data[] = (byte[])value;
        return data;
    }
    
    /**
     * Convenience method that returns the value of the given option 
     * as a string. The byte array for the specified option will be
     * obtained and converted into a string. Returns <code>null</code> if 
     * the specified option is unknown or not a <code>byte[]</code> value.
     * 
     * @param key An option identifier
     * @return The option value
     */
    public String getString(int key)
    {
        byte data[] = getBytes(key);
        if (data == null)
        {
            return null;
        }
        return createString(data);
    }
    
    /**
     * Converts the given byte array into a String. The characters
     * of the given data are connected to a String, until a 
     * terminating '\0' character is encountered.
     * 
     * @param data The byte array
     * @return The String
     */
    private static String createString(byte data[])
    {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<data.length; i++)
        {   
            if (data[i] == 0)
            {
                break;
            }
            sb.append((char)data[i]);
        }
        String result = sb.toString();
        return result;
    }
    
    
    /**
     * Returns a String representation of this object.
     *
     * @return A String representation of this object.
     */
    @Override
    public String toString()
    {
        return "JITOptions["+createString(",")+"]";
        //return toFormattedString();
    }

    /**
     * Creates and returns a formatted (aligned, multi-line) String
     * representation of this object
     *
     * @return A formatted String representation of this object
     */
    public String toFormattedString()
    {
        return "JITOptions:\n    "+createString("\n    ");
    }

    /**
     * Creates and returns a string representation of this object,
     * using the given separator for the options
     *
     * @return A String representation of this object
     */
    private String createString(String f)
    {
        StringBuffer sb = new StringBuffer();
        int keys[] = getKeys();
        for (int i=0; i<keys.length; i++)
        {
            int key = keys[i];
            sb.append(CUjit_option.stringFor(key)+"=");
            Object value = map.get(key);
            if (value instanceof byte[])
            {
                byte data[] = (byte[])value;
                sb.append(createString(data));
            }
            else
            {
                sb.append(String.valueOf(value));
            }
            if (i<keys.length-1)
            {
                sb.append(f);
            }
        }
        return sb.toString();
    }
    
    
}
