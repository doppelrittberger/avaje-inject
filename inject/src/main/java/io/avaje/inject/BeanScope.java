package io.avaje.inject;

import io.avaje.lang.NonNullApi;
import io.avaje.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Holds beans created by dependency injection.
 * <p>
 * The beans have singleton scope, support lifecycle methods for postConstruct and
 * preDestroy and are created (wired) via dependency injection.
 * </p>
 *
 * <h3>Create a BeanScope</h3>
 * <p>
 * We can programmatically create a BeanScope via {@code BeanScope.newBuilder()}.
 * </p>
 * <pre>{@code
 *
 *   // create a BeanScope ...
 *
 *   try (BeanScope scope = BeanScope.newBuilder()
 *     .build()) {
 *
 *     CoffeeMaker coffeeMaker = context.get(CoffeeMaker.class);
 *     coffeeMaker.makeIt()
 *   }
 *
 * }</pre>
 *
 * <h3>External dependencies</h3>
 * <p>
 * We can supporting external dependencies when creating the BeanScope. We need to do 2 things.
 * we need to specify these via
 * </p>
 * <ul>
 *   <li>
 *       1. Specify the external dependency via {@code @InjectModule(requires=...)}.
 *       Otherwise at compile time the annotation processor detects it as a missing dependency and we can't compile.
 *   </li>
 *   <li>
 *       2. Provide the dependency when creating the BeanScope
 *   </li>
 * </ul>
 * <p>
 * For example, given we have Pump as an externally provided dependency.
 *
 * <pre>{@code
 *
 *   // tell the annotation processor Pump is provided externally
 *   // otherwise it thinks we have a missing dependency
 *
 *   @InjectModule(requires=Pump.class)
 *
 * }</pre>
 * <p>
 * When we build the BeanScope provide the dependency via {@link BeanScopeBuilder#withBean(Class, Object)}.
 *
 * <pre>{@code
 *
 *   // provide external dependencies ...
 *   Pump pump = ...
 *
 *   try (BeanScope scope = BeanScope.newBuilder()
 *     .withBean(Pump.class, pump)
 *     .build()) {
 *
 *     CoffeeMaker coffeeMaker = context.get(CoffeeMaker.class);
 *     coffeeMaker.makeIt()
 *   }
 *
 * }</pre>
 */
@NonNullApi
public interface BeanScope extends AutoCloseable {

  /**
   * Build a bean scope with options for shutdown hook and supplying external dependencies.
   * <p>
   * We can optionally:
   * <ul>
   *   <li>Provide external dependencies</li>
   *   <li>Specify a parent BeanScope</li>
   *   <li>Specify specific modules to wire</li>
   *   <li>Specify to include a shutdown hook (to fire preDestroy lifecycle methods)</li>
   *   <li>Use {@code forTesting()} to specify mocks and spies to use when wiring tests</li>
   * </ul>
   *
   * <pre>{@code
   *
   *   // create a BeanScope ...
   *
   *   try (BeanScope scope = BeanScope.newBuilder()
   *     .build()) {
   *
   *     CoffeeMaker coffeeMaker = context.get(CoffeeMaker.class);
   *     coffeeMaker.makeIt()
   *   }
   *
   * }</pre>
   */
  static BeanScopeBuilder newBuilder() {
    return new DBeanScopeBuilder();
  }

  /**
   * Return a single bean given the type.
   *
   * <pre>{@code
   *
   *   CoffeeMaker coffeeMaker = beanScope.get(CoffeeMaker.class);
   *   coffeeMaker.brew();
   *
   * }</pre>
   *
   * @param type an interface or bean type
   * @throws java.util.NoSuchElementException When no matching bean is found
   */
  <T> T get(Class<T> type);

  /**
   * Return a single bean given the type and name.
   *
   * <pre>{@code
   *
   *   Heater heater = beanScope.get(Heater.class, "electric");
   *   heater.heat();
   *
   * }</pre>
   *
   * @param type an interface or bean type
   * @param name the name qualifier of a specific bean
   * @throws java.util.NoSuchElementException When no matching bean is found
   */
  <T> T get(Class<T> type, @Nullable String name);

  /**
   * Return a single bean given the generic type and name.
   *
   * @param type The generic type
   * @param name the name qualifier of a specific bean
   * @throws java.util.NoSuchElementException When no matching bean is found
   */
  <T> T get(Type type, @Nullable String name);

  /**
   * Return the list of beans that have an annotation.
   *
   * <pre>{@code
   *
   *   // e.g. register all controllers with web a framework
   *   // .. where Controller is an annotation on the beans
   *
   *   List<Object> controllers = beanScope.listByAnnotation(Controller.class);
   *
   * }</pre>
   *
   * @param annotation An annotation class.
   */
  List<Object> listByAnnotation(Class<?> annotation);

  /**
   * Return the list of beans that implement the interface.
   *
   * <pre>{@code
   *
   *   // e.g. register all routes for a web framework
   *
   *   List<WebRoute> routes = beanScope.list(WebRoute.class);
   *
   * }</pre>
   *
   * @param interfaceType An interface class.
   */
  <T> List<T> list(Class<T> interfaceType);

  /**
   * Return the list of beans that implement the interface sorting by priority.
   */
  <T> List<T> listByPriority(Class<T> interfaceType);

  /**
   * Return the beans that implement the interface sorting by the priority annotation used.
   * <p>
   * The priority annotation will typically be either <code>javax.annotation.Priority</code>
   * or <code>jakarta.annotation.Priority</code>.
   *
   * @param interfaceType The interface type of the beans to return
   * @param priority      The priority annotation used to sort the beans
   */
  <T> List<T> listByPriority(Class<T> interfaceType, Class<? extends Annotation> priority);

  /**
   * Return all the bean entries from the scope.
   * <p>
   * The bean entries include entries from the parent scope if it has one.
   *
   * @return All bean entries from the scope.
   */
  List<BeanEntry> all();

  /**
   * Close the scope firing any <code>@PreDestroy</code> lifecycle methods.
   */
  void close();
}
