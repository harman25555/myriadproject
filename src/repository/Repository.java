package repository;

import exception.StorageException;

import java.util.List;
import java.util.Optional;

public interface Repository<T> {

    Optional<T> findById(String id) throws StorageException;

    List<T> findAll() throws StorageException;

    void save(T entity) throws StorageException;

    void delete(String id) throws StorageException;
}