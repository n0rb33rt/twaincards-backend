package com.norbert.twaincards.service;

import com.norbert.twaincards.dto.TagDTO;
import com.norbert.twaincards.entity.Tag;
import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.exception.ResourceAlreadyExistsException;
import com.norbert.twaincards.exception.ResourceNotFoundException;
import com.norbert.twaincards.repository.TagRepository;
import com.norbert.twaincards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {

  private final TagRepository tagRepository;
  private final ModelMapper modelMapper;
  private final SecurityUtils securityUtils;

  @Transactional(readOnly = true)
  public List<TagDTO> getAllTags() {
    User currentUser = securityUtils.getCurrentUser();
    return tagRepository.findByUser(currentUser).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public TagDTO getTagById(Long id) {
    User currentUser = securityUtils.getCurrentUser();
    Tag tag = tagRepository.findByIdAndUser(id, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));
    return convertToDto(tag);
  }

  @Transactional(readOnly = true)
  public TagDTO getTagByName(String name) {
    User currentUser = securityUtils.getCurrentUser();
    Tag tag = tagRepository.findByNameAndUser(name, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with name: " + name));
    return convertToDto(tag);
  }

  @Transactional(readOnly = true)
  public List<TagDTO> searchTags(String name) {
    User currentUser = securityUtils.getCurrentUser();
    return tagRepository.findByNameContainingIgnoreCaseAndUser(name, currentUser).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
  }


  @Transactional(readOnly = true)
  public List<TagDTO> getMostPopularTags(int limit) {
    User currentUser = securityUtils.getCurrentUser();
    List<Object[]> popularTags = tagRepository.findMostPopularTagsByUser(currentUser, PageRequest.of(0, limit));

    return popularTags.stream()
            .map(row -> {
              Tag tag = (Tag) row[0];
              Long cardCount = (Long) row[1];

              TagDTO tagDTO = convertToDto(tag);
              tagDTO.setCardCount(cardCount.intValue());
              return tagDTO;
            })
            .collect(Collectors.toList());
  }

  @Transactional
  public TagDTO createTag(TagDTO tagDTO) {
    User currentUser = securityUtils.getCurrentUser();

    if (tagRepository.existsByNameAndUser(tagDTO.getName(), currentUser)) {
      throw new ResourceAlreadyExistsException("Tag already exists with name: " + tagDTO.getName());
    }

    Tag tag = Tag.builder()
            .name(tagDTO.getName())
            .user(currentUser)
            .build();

    Tag savedTag = tagRepository.save(tag);
    log.info("Tag created successfully with id: {}", savedTag.getId());

    return convertToDto(savedTag);
  }

  @Transactional
  public TagDTO updateTag(Long id, TagDTO tagDTO) {
    User currentUser = securityUtils.getCurrentUser();

    Tag tag = tagRepository.findByIdAndUser(id, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));

    if (!tag.getName().equals(tagDTO.getName()) &&
            tagRepository.existsByNameAndUser(tagDTO.getName(), currentUser)) {
      throw new ResourceAlreadyExistsException("Another tag already exists with name: " + tagDTO.getName());
    }

    tag.setName(tagDTO.getName());

    Tag updatedTag = tagRepository.save(tag);
    log.info("Tag updated successfully with id: {}", updatedTag.getId());

    return convertToDto(updatedTag);
  }

  @Transactional
  public void deleteTag(Long id) {
    User currentUser = securityUtils.getCurrentUser();

    Tag tag = tagRepository.findByIdAndUser(id, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));

    tagRepository.delete(tag);
    log.info("Tag deleted successfully with id: {}", id);
  }

  private TagDTO convertToDto(Tag tag) {
    TagDTO tagDTO = modelMapper.map(tag, TagDTO.class);
    tagDTO.setCardCount(tag.getCards().size());
    return tagDTO;
  }
}