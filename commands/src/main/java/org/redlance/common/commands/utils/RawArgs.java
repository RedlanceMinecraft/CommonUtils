package org.redlance.common.commands.utils;

import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public final class RawArgs implements IParameterConsumer {
    @Override
    public void consumeParameters(Stack<String> args, ArgSpec argSpec, CommandSpec commandSpec) {
        List<String> all = new ArrayList<>();
        while (!args.isEmpty()) {
            all.add(args.pop());
        }
        argSpec.setValue(all.toArray(new String[0]));
    }
}
