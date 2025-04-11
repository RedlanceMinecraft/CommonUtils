package org.redlance.common.utils.requester.mojang.namehistory;

import org.redlance.common.CommonUtils;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.mojang.namehistory.obj.Username;
import org.redlance.common.utils.requester.mojang.namehistory.providers.CraftyProvider;
import org.redlance.common.utils.requester.mojang.namehistory.providers.ConvertingMojangProvider;
import org.redlance.common.utils.requester.mojang.namehistory.providers.LabyProvider;

import java.io.IOException;
import java.util.*;

public class NameHistoryRequester implements INameProvider {
    public static final NameHistoryRequester INSTANCE = new NameHistoryRequester();

    private final Map<Class<? extends INameProvider>, INameProvider> providers = new HashMap<>();
    protected NameHistoryRequester() {
        registerProvider(new CraftyProvider());
        registerProvider(new LabyProvider());

        // Mojang
        registerProvider(new ConvertingMojangProvider("authserver.ely.by/api"));
    }

    @Override
    public List<Username> getNameHistoryByName(String name) {
        List<Username> names = new ArrayList<>();

        Requester.prepareParallelRequests(this.providers.keySet().stream(), provider -> {
            try {
                return getNameHistoryByName(provider, name);
            } catch (Throwable th) {
                CommonUtils.LOGGER.warn("Failed to get name history by uuid via {}!", getProviderByClass(provider), th);
                return Collections.<Username>emptyList();
            }
        }).forEach(names::addAll);

        return names;
    }

    public List<Username> getNameHistoryByName(Class<? extends INameProvider> provider, String name) throws IOException, InterruptedException {
        return this.providers.get(provider).getNameHistoryByName(name);
    }

    @Override
    public List<Username> getNameHistoryById(String uuid) {
        List<Username> names = new ArrayList<>();

        Requester.prepareParallelRequests(this.providers.keySet().stream(), provider -> {
            try {
                return getNameHistoryById(provider, uuid);
            } catch (Throwable th) {
                CommonUtils.LOGGER.warn("Failed to get name history by uuid via {}!", getProviderByClass(provider), th);
                return Collections.<Username>emptyList();
            }
        }).forEach(names::addAll);

        return names;
    }

    public List<Username> getNameHistoryById(Class<? extends INameProvider> provider, String uuid) throws IOException, InterruptedException {
        return this.providers.get(provider).getNameHistoryById(uuid);
    }

    public void registerProvider(INameProvider provider) {
        this.providers.put(provider.getClass(), provider);
    }

    protected INameProvider getProviderByClass(Class<? extends INameProvider> clazz) {
        return this.providers.get(clazz);
    }
}
