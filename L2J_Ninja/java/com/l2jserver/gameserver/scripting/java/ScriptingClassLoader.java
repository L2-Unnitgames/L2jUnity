/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.scripting.java;

import java.util.logging.Logger;

/**
 * @author HorridoJoho
 */
public final class ScriptingClassLoader extends ClassLoader
{
	public static final Logger LOGGER = Logger.getLogger(ScriptingClassLoader.class.getName());
	
	private Iterable<ScriptingOutputFileObject> _compiledClasses;
	
	ScriptingClassLoader(final ClassLoader parent, final Iterable<ScriptingOutputFileObject> compiledClasses)
	{
		super(parent);
		_compiledClasses = compiledClasses;
	}
	
	void removeCompiledClasses()
	{
		_compiledClasses = null;
	}
	
	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException
	{
		for (final ScriptingOutputFileObject compiledClass : _compiledClasses)
		{
			if (compiledClass.getJavaName().equals(name))
			{
				final byte[] classBytes = compiledClass.getJavaData();
				return defineClass(name, classBytes, 0, classBytes.length);
			}
		}
		
		return super.findClass(name);
	}
}
