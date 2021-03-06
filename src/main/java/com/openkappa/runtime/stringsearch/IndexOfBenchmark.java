package com.openkappa.runtime.stringsearch;

import org.openjdk.jmh.annotations.Benchmark;

public class IndexOfBenchmark {

    @Benchmark
    public int indexOfSearcher(SearchState searchState) {
        return searchState.searcher.find(searchState.next());
    }
    
}
