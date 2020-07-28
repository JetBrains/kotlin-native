public class ExtendsFoo extends Foo{
  @Override
  public int add2(int a, int b) {
    return a - b;
  }

  public int add3(int firstparam, int secondparam, int thirdparam) {
    return this.add2(firstparam, this.add2(secondparam, thirdparam));
  }

  public Foo returnFoo() {
    return new Foo();
  }
}
