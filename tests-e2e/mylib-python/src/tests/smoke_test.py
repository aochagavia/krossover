import pytest
from concurrent.futures import ThreadPoolExecutor

from mylib import *

def test_divide_by_zero():
    result = Math.divide(1, 0)
    assert isinstance(result, MyResult.Failure)
    assert result.message() == "attempted to divide by zero"

def test_divide_success():
    result = Math.divide(84, 2)
    assert isinstance(result, MyResult.Success)
    assert result.value() == 42

def test_get_map():
    map = MapFunctions.get_map()
    assert map == {
        "the answer": "42",
        "hello": "world"
    }

def test_process_map():
    map = {
        "name": "John Doe",
        "age": "42"
    }
    assert MapFunctions.process_map(map) == "name: John Doe, age: 42"

def test_create_non_zero_error():
    result = NonZeroInt.from_int(0)
    assert isinstance(result, MyResult.Failure)
    assert result.message() == "attempted to create `NonZeroInt`, but the provided value was `0`"

def test_create_non_zero_success():
    result = NonZeroInt.from_int(33)
    assert isinstance(result, MyResult.Success)
    assert result.value() == 33

def test_different_threads():
    with ThreadPoolExecutor() as executor:
        result = executor.submit(lambda: NonZeroInt.from_int(33)).result()
        assert isinstance(result, MyResult.Success)
        value = executor.submit(lambda: result.value()).result()
        assert value == 33

def test_eq_hash():
    # Each class has its own instance
    result1 = NonZeroInt.from_int(33)
    result2 = NonZeroInt.from_int(33)
    assert result1 != result2
    assert hash(result1) != hash(result2)

    # Objects are singletons
    object1 = ChildObject()
    object2 = ChildObject()
    assert object1 == object2
    assert hash(object1) == hash(object2)
