use super::*;

#[test]
fn test_divide_by_zero() {
    let result = Math::divide(1, 0);
    match result {
        MyResult::Failure(failure) => {
            assert_eq!(failure.message(), "attempted to divide by zero");
        }
        _ => panic!("expected Failure"),
    }
}

#[test]
fn test_divide_success() {
    let result = Math::divide(84, 2);
    match result {
        MyResult::Success(success) => {
            assert_eq!(success.value(), 42);
        }
        _ => panic!("expected Success"),
    }
}

#[test]
fn test_create_non_zero_error() {
    let result = NonZeroInt::from_int(0);
    match result {
        MyResult::Failure(failure) => {
            assert_eq!(
                failure.message(),
                "attempted to create `NonZeroInt`, but the provided value was `0`"
            );
        }
        _ => panic!("expected Failure"),
    }
}

#[test]
fn test_create_non_zero_success() {
    let result = NonZeroInt::from_int(33);
    match result {
        MyResult::Success(success) => {
            assert_eq!(success.value(), 33);
        }
        _ => panic!("expected Success"),
    }
}

#[test]
fn test_different_threads() {
    let result = std::thread::spawn(|| NonZeroInt::from_int(33))
        .join()
        .unwrap();
    match result {
        MyResult::Success(success) => {
            let value = std::thread::spawn(move || success.value())
                .join()
                .unwrap();
            assert_eq!(value, 33);
        }
        _ => panic!("expected Success"),
    }
}
