extern void *resolve_symbol(const char*);

int
run_test() {
  int (*when_through)(int) = resolve_symbol("kfun:when_through");

  if (when_through(2) != 3) return 1;

  return 0;
}
