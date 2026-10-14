package com.mrcrayfish.mightymail.platform;

import com.mrcrayfish.mightymail.platform.services.IPlatformHelper;

/**
 * Author: MrCrayfish
 */
public class Services
{
    public static final IPlatformHelper PLATFORM = ClientServices.load(IPlatformHelper.class);
}
