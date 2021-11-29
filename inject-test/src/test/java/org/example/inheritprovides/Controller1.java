package org.example.inheritprovides;

import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class Controller1 extends Controller {

  @Override
  Map<String, String> getContext() {
    Map<String, String> context = super.getContext();
    context.put("something", "1");
    return context;
  }

}
