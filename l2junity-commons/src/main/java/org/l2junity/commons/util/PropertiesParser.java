/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.commons.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplifies loading of property files and adds logging if a non existing property is requested.
 * @author NosBit
 */
public final class PropertiesParser
{
	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesParser.class);
	
	private final Properties _properties = new Properties();
	private final File _file;
	
	public PropertiesParser(String name)
	{
		this(new File(name));
	}
	
	public PropertiesParser(File file)
	{
		_file = file;
		try (FileInputStream fileInputStream = new FileInputStream(file))
		{
			try (InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, Charset.defaultCharset()))
			{
				_properties.load(inputStreamReader);
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("[{}] There was an error loading config", _file.getName(), e);
		}
	}
	
	public boolean containskey(String key)
	{
		return _properties.containsKey(key);
	}
	
	private String getValue(String key)
	{
		String value = _properties.getProperty(key);
		return value != null ? value.trim() : null;
	}
	
	public boolean getBoolean(String key, boolean defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValue);
			return defaultValue;
		}
		
		if (value.equalsIgnoreCase("true"))
		{
			return true;
		}
		else if (value.equalsIgnoreCase("false"))
		{
			return false;
		}
		else
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {} should be \"boolean\" using default value: {}", _file.getName(), key, value, defaultValue);
			return defaultValue;
		}
	}
	
	public byte getByte(String key, byte defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValue);
			return defaultValue;
		}
		
		try
		{
			return Byte.parseByte(value);
		}
		catch (NumberFormatException e)
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {} should be \"byte\" using default value: {}", _file.getName(), key, value, defaultValue);
			return defaultValue;
		}
	}
	
	public short getShort(String key, short defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValue);
			return defaultValue;
		}
		
		try
		{
			return Short.parseShort(value);
		}
		catch (NumberFormatException e)
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {} should be \"short\" using default value: {}", _file.getName(), key, value, defaultValue);
			return defaultValue;
		}
	}
	
	public int getInt(String key, int defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValue);
			return defaultValue;
		}
		
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {} should be \"int\" using default value: {}", _file.getName(), key, value, defaultValue);
			return defaultValue;
		}
	}
	
	public long getLong(String key, long defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValue);
			return defaultValue;
		}
		
		try
		{
			return Long.parseLong(value);
		}
		catch (NumberFormatException e)
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {} should be \"long\" using default value: {}", _file.getName(), key, value, defaultValue);
			return defaultValue;
		}
	}
	
	public float getFloat(String key, float defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValue);
			return defaultValue;
		}
		
		try
		{
			return Float.parseFloat(value);
		}
		catch (NumberFormatException e)
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {} should be \"float\" using default value: {}", _file.getName(), key, value, defaultValue);
			return defaultValue;
		}
	}
	
	public double getDouble(String key, double defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValue);
			return defaultValue;
		}
		
		try
		{
			return Double.parseDouble(value);
		}
		catch (NumberFormatException e)
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {} should be \"double\" using default value: {}", _file.getName(), key, value, defaultValue);
			return defaultValue;
		}
	}
	
	public String getString(String key, String defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValue);
			return defaultValue;
		}
		return value;
	}
	
	public <T extends Enum<T>> T getEnum(String key, Class<T> clazz, T defaultValue)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValue);
			return defaultValue;
		}
		
		try
		{
			return Enum.valueOf(clazz, value);
		}
		catch (IllegalArgumentException e)
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {} should be enum value of \"{}\" using default value: {}", _file.getName(), key, value, clazz.getSimpleName(), defaultValue);
			return defaultValue;
		}
	}
	
	/**
	 * @param durationPattern
	 * @param defaultValue
	 * @return {@link Duration} object by the durationPattern specified, {@code null} in case of malformed pattern.
	 */
	public Duration getDuration(String durationPattern, String defaultValue)
	{
		return getDuration(durationPattern, defaultValue, null);
	}
	
	/**
	 * @param durationPattern
	 * @param defaultValue
	 * @param defaultDuration
	 * @return {@link Duration} object by the durationPattern specified, the defaultDuration in case of malformed pattern.
	 */
	public Duration getDuration(String durationPattern, String defaultValue, Duration defaultDuration)
	{
		final String value = getString(durationPattern, defaultValue);
		try
		{
			return TimeUtil.parseDuration(value);
		}
		catch (IllegalStateException e)
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {] should be time patttern using default value: {}", _file.getName(), durationPattern, value, defaultValue);
		}
		return defaultDuration;
	}
	
	/**
	 * @param key
	 * @param separator
	 * @param defaultValues
	 * @return
	 */
	public int[] getIntArray(String key, String separator, int... defaultValues)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValues);
			return defaultValues;
		}
		
		try
		{
			final String[] data = value.trim().split(separator);
			int[] result = new int[data.length];
			for (int i = 0; i < data.length; i++)
			{
				result[i] = Integer.decode(data[i].trim());
			}
			return result;
		}
		catch (IllegalArgumentException e)
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {} should be array using default value: {}", _file.getName(), key, value, defaultValues);
			return defaultValues;
		}
	}
	
	/**
	 * @param <T>
	 * @param key
	 * @param separator
	 * @param clazz
	 * @param defaultValues
	 * @return
	 */
	@SafeVarargs
	public final <T extends Enum<T>> List<T> getEnumList(String key, String separator, Class<T> clazz, T... defaultValues)
	{
		String value = getValue(key);
		if (value == null)
		{
			LOGGER.warn("[{}] missing property for key: {} using default value: {}", _file.getName(), key, defaultValues);
			return Arrays.asList(defaultValues);
		}
		
		try
		{
			final String[] data = value.trim().split(separator);
			final List<T> result = new ArrayList<>(data.length);
			for (String element : data)
			{
				result.add(Enum.valueOf(clazz, element));
			}
			return result;
		}
		catch (IllegalArgumentException e)
		{
			LOGGER.warn("[{}] Invalid value specified for key: {} specified value: {} should be array using default value: {}", _file.getName(), key, value, defaultValues);
			return Arrays.asList(defaultValues);
		}
	}
}
