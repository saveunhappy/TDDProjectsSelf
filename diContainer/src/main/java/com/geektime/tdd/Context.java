package com.geektime.tdd;

import java.util.Optional;

public interface Context {
    Optional get(Ref ref);
}
