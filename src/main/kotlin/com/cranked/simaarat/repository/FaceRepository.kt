package com.cranked.simaarat.repository

import com.cranked.simaarat.dto.Face
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FaceRepository : JpaRepository<Face, String>{
    fun findByFaceId(id: String): Face?
}