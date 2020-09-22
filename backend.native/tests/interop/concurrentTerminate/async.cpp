#include <thread>
#include <future>
#include <chrono>
#include <vector>
#include <csignal>  // signal.h

#include "async.h"

int test_ConcurrentTerminate() {
    signal(SIGABRT, *[](int){ exit(0); }); // Windows does not have sigaction

    std::vector<std::future<void>> futures;

    for (size_t i = 0; i < 100; ++i) {
        futures.emplace_back(std::async(std::launch::async,
                [](size_t param) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(param));
                    throw std::runtime_error("Reporting error!");
                },
                200 - i));
    }

    for (auto &future : futures) future.get();
    return 0;
}
