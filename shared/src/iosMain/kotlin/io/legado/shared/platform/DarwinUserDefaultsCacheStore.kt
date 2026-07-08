package io.legado.shared.platform

import platform.Foundation.NSUserDefaults

class DarwinUserDefaultsCacheStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : CacheStorePort {

    override fun getText(key: String): String? {
        return defaults.stringForKey(key)
    }

    override fun putText(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
