/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.proxy.util.chat;

import de.timesnake.library.chat.Code;
import de.timesnake.library.commands.Completion;
import de.timesnake.library.commands.extended.ExArguments;

import java.util.Collection;

public class ExCompletion extends Completion<ExCompletion, Sender, Argument, ExArguments<Argument>> {

  public ExCompletion() {
  }

  public ExCompletion(Code permission) {
    super(permission);
  }

  public ExCompletion(Collection<String> values) {
    super(values);
  }

  public ExCompletion(CompleteSupplier<Sender, Argument, ExArguments<Argument>, Collection<String>> valuesProvider) {
    super(valuesProvider);
  }

  public ExCompletion(Code permission, Collection<String> values) {
    super(permission, values);
  }

  public ExCompletion(Code permission,
                      CompleteSupplier<Sender, Argument, ExArguments<Argument>, Collection<String>> valuesProvider) {
    super(permission, valuesProvider);
  }
}
