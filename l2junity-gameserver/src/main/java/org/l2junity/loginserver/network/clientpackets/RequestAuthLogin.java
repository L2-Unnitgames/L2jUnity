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
package org.l2junity.loginserver.network.clientpackets;

import java.net.InetAddress;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;

import org.l2junity.Config;
import org.l2junity.loginserver.GameServerTable.GameServerInfo;
import org.l2junity.loginserver.LoginController;
import org.l2junity.loginserver.LoginController.AuthLoginResult;
import org.l2junity.loginserver.model.data.AccountInfo;
import org.l2junity.loginserver.network.L2LoginClient;
import org.l2junity.loginserver.network.L2LoginClient.LoginClientState;
import org.l2junity.loginserver.network.serverpackets.AccountKicked;
import org.l2junity.loginserver.network.serverpackets.AccountKicked.AccountKickedReason;
import org.l2junity.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import org.l2junity.loginserver.network.serverpackets.LoginOk;
import org.l2junity.loginserver.network.serverpackets.LoginOtpFail;
import org.l2junity.loginserver.network.serverpackets.ServerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * Format: x 0 (a leading null) x: the rsa encrypted block with the login an password.
 * 
 * <pre>
 */
public class RequestAuthLogin extends L2LoginClientPacket
{
	private static Logger _log = LoggerFactory.getLogger(RequestAuthLogin.class);
	
	private final byte[] _raw1 = new byte[128];
	private final byte[] _raw2 = new byte[128];
	private boolean _newAuthMethod = false;
	
	private String _user;
	private String _password;
	private int _ncotp;
	
	/**
	 * @return
	 */
	public String getPassword()
	{
		return _password;
	}
	
	/**
	 * @return
	 */
	public String getUser()
	{
		return _user;
	}
	
	public int getOneTimePassword()
	{
		return _ncotp;
	}
	
	@Override
	public boolean readImpl()
	{
		if (super._buf.remaining() >= 256)
		{
			_newAuthMethod = true;
			readB(_raw1);
			readB(_raw2);
			return true;
		}
		else if (super._buf.remaining() >= 128)
		{
			readB(_raw1);
			return true;
		}
		return false;
	}
	
	@Override
	public void run()
	{
		final L2LoginClient client = getClient();
		byte[] decrypted = new byte[_newAuthMethod ? 256 : 128];
		try
		{
			final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.DECRYPT_MODE, client.getRSAPrivateKey());
			rsaCipher.doFinal(_raw1, 0, 128, decrypted, 0);
			if (_newAuthMethod)
			{
				rsaCipher.doFinal(_raw2, 0, 128, decrypted, 128);
			}
		}
		catch (GeneralSecurityException e)
		{
			_log.info("", e);
			return;
		}
		
		try
		{
			if (_newAuthMethod)
			{
				_user = new String(decrypted, 0x4E, 50).trim() + new String(decrypted, 0xCE, 14).trim();
				_password = new String(decrypted, 0xDC, 16).trim();
				_ncotp = (decrypted[0xFC] & 0xFF) | ((decrypted[0xFD] & 0xFF) << 8) | ((decrypted[0xFE] & 0xFF) << 16) | ((decrypted[0xFF] & 0xFF) << 24);
			}
			else
			{
				_user = new String(decrypted, 0x5E, 14).trim();
				_password = new String(decrypted, 0x6C, 16).trim();
				_ncotp = (decrypted[0x7C] & 0xFF) | ((decrypted[0x7D] & 0xFF) << 8) | ((decrypted[0x7E] & 0xFF) << 16) | ((decrypted[0x7F] & 0xFF) << 24);
			}
		}
		catch (Exception e)
		{
			_log.warn("", e);
			return;
		}
		
		final InetAddress clientAddr = getClient().getConnection().getInetAddress();
		final LoginController lc = LoginController.getInstance();
		final AccountInfo info = lc.retriveAccountInfo(clientAddr, _user, _password);
		if (info == null)
		{
			// user or pass wrong
			client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
			return;
		}
		else if (!info.checkOTP(_ncotp))
		{
			client.sendPacket(new LoginOtpFail());
			client.close(LoginFailReason.REASON_ACCOUNT_INFO_INCORRECT_CONTACT_SUPPORT);
			return;
		}
		
		final AuthLoginResult result = lc.tryCheckinAccount(client, clientAddr, info);
		switch (result)
		{
			case AUTH_SUCCESS:
				client.setAccount(info.getLogin());
				client.setState(LoginClientState.AUTHED_LOGIN);
				client.setSessionKey(lc.assignSessionKeyToClient(info.getLogin(), client));
				lc.getCharactersOnAccount(info.getLogin());
				if (Config.SHOW_LICENCE)
				{
					client.sendPacket(new LoginOk(getClient().getSessionKey()));
				}
				else
				{
					getClient().sendPacket(new ServerList(getClient()));
				}
				break;
			case INVALID_PASSWORD:
				client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
				break;
			case ACCOUNT_BANNED:
				client.close(new AccountKicked(AccountKickedReason.REASON_PERMANENTLY_BANNED));
				return;
			case ALREADY_ON_LS:
				final L2LoginClient oldClient = lc.getAuthedClient(info.getLogin());
				if (oldClient != null)
				{
					// kick the other client
					oldClient.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
					lc.removeAuthedLoginClient(info.getLogin());
				}
				// kick also current client
				client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
				break;
			case ALREADY_ON_GS:
				final GameServerInfo gsi = lc.getAccountOnGameServer(info.getLogin());
				if (gsi != null)
				{
					client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
					
					// kick from there
					if (gsi.isAuthed())
					{
						gsi.getGameServerThread().kickPlayer(info.getLogin());
					}
				}
				break;
		}
	}
}
