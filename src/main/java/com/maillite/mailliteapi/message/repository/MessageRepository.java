package com.maillite.mailliteapi.message.repository;

import com.maillite.mailliteapi.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Inbox paginada do usuário autenticado (RF05). A ordenação vem do Pageable (o
     * controller aplica sentAt DESC como padrão), então não é fixada aqui na query.
     */
    Page<Message> findByRecipientId(Long recipientId, Pageable pageable);

    /**
     * Busca por ID validando que o usuário autenticado é o destinatário — garante que um
     * usuário não acesse/exclua mensagem de outra caixa de entrada via GET/DELETE /messages/{id}.
     */
    @Query("""
            SELECT m FROM Message m
            WHERE m.id = :id AND m.recipient.id = :recipientId
            """)
    Optional<Message> findByIdAndRecipientId(@Param("id") Long id, @Param("recipientId") Long recipientId);
}