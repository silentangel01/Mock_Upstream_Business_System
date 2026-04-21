package com.mubs.repository

import com.mubs.model.RoundRobinCounter
import org.springframework.data.mongodb.repository.MongoRepository

interface RoundRobinCounterRepository : MongoRepository<RoundRobinCounter, String>
