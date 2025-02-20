package org.example.coffee.generic;

import io.avaje.inject.ApplicationScope;
import io.avaje.inject.BeanScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class HazManagerTest {

  @Test
  public void find_when_allWired() {
    HazManager hazManager = ApplicationScope.get(HazManager.class);
    Haz haz = hazManager.find(42L);

    assertThat(haz.id).isEqualTo(42L);
  }

  @Test
  public void find_with_mockHaz() {
    try (BeanScope context = BeanScope.newBuilder()
      .forTesting()
      .withMock(HazRepo.class)
      .build()) {

      HazManager hazManager = context.get(HazManager.class);
      Haz haz = hazManager.find(42L);

      assertThat(haz).isNull();
    }
  }

  @Test
  public void find_with_stubHazUsingMockito() {
    try (BeanScope context = BeanScope.newBuilder()
      .forTesting()
      .withMock(HazRepo.class, hazRepo -> {
        when(hazRepo.findById(anyLong())).thenReturn(new Haz(-23L));
      })
      .build()) {

      HazManager hazManager = context.get(HazManager.class);
      Haz haz = hazManager.find(42L);

      assertThat(haz.id).isEqualTo(-23L);
    }
  }

  @Test
  public void withBean_usingGenericType() {
    TDFoo testDouble = new TDFoo();

    try (BeanScope context = BeanScope.newBuilder()
      .withBean(HazRepo$DI.TYPE_RepositoryHazLong, testDouble)
      .build()) {

      HazManager hazManager = context.get(HazManager.class);
      Haz haz = hazManager.find(42L);

      assertThat(haz.id).isEqualTo(7L);
    }
  }

  @Test
  public void find_with_testDouble() {
    TDHazRepo testDouble = new TDHazRepo();

    try (BeanScope context = BeanScope.newBuilder()
      .withBeans(testDouble)
      .build()) {

      HazManager hazManager = context.get(HazManager.class);

      Haz haz = hazManager.find(42L);
      assertThat(haz.id).isEqualTo(64L);

      testDouble.id = 48L;
      haz = hazManager.find(42L);
      assertThat(haz.id).isEqualTo(48L);

      testDouble.id = 128L;
      haz = hazManager.find(42L);
      assertThat(haz.id).isEqualTo(128L);

    }
  }

  static class TDFoo implements Repository<Haz, Long> {

    @Override
    public Haz findById(Long id) {
      return new Haz(7L);
    }
  }

  /**
   * Test double for HazRepo - nice when we want interesting test behaviour.
   */
  static class TDHazRepo extends HazRepo {

    long id = 64L;

    @Override
    public Haz findById(Long paramId) {
      return new Haz(id);
    }
  }
}
