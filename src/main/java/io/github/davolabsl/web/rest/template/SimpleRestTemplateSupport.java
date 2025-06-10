package io.github.davolabsl.web.rest.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.davolabsl.dao.GenericDao;
import io.github.davolabsl.dao.support.FastPage;
import io.github.davolabsl.web.rest.template.dto.ApiResponseDto;
import io.github.davolabsl.web.rest.template.dto.PaginationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SimpleRestTemplateSupport<MD, ME, RD, ID extends Serializable> {

    @Autowired
    private GenericDao genericDao;

    @Autowired
    private ObjectMapper objectMapper;

    public ApiResponseDto<RD> create(MD mainDto, ME mainEntity, RD responseDto) {
        ME me = mapDtoToEntity(mainDto, mainEntity);
        ME savedMainEntity = (ME) genericDao.save(me);
        RD rd = mapEntityToDto(savedMainEntity, responseDto);
        ApiResponseDto<RD> apiResponseDto = new ApiResponseDto<>();
        apiResponseDto.setResult(rd);
        return apiResponseDto;
    }

    public ApiResponseDto<List<RD>> findAll(ME mainEntity, RD responseDto) {
        List<ME> all = genericDao.findAll(mainEntity);
        List<RD> collect = all.stream().map(me -> mapEntityToDto(me, responseDto)).collect(Collectors.toList());
        ApiResponseDto<List<RD>> apiResponseDto = new ApiResponseDto<>();
        apiResponseDto.setResult(collect);
        return apiResponseDto;
    }

    public ApiResponseDto<RD> findById(ME mainEntity, RD responseDto, ID pk) {
        Optional<ME> me = genericDao.findById(mainEntity, pk);
        RD rd = mapEntityToDto(me.get(), responseDto);
        ApiResponseDto<RD> apiResponseDto = new ApiResponseDto<>();
        apiResponseDto.setResult(rd);
        return apiResponseDto;
    }

    public ApiResponseDto<List<RD>> paginate(ME mainEntity, RD responseDto, ID page, ID size) {
        FastPage paginate = genericDao.paginate(mainEntity, page, size);
        List<ME> meList = (List<ME>) paginate.getResults();
        List<RD> collect = meList.stream().map(me -> mapEntityToDto(me, responseDto)).collect(Collectors.toList());
        ApiResponseDto<List<RD>> apiResponseDto = new ApiResponseDto<>();
        apiResponseDto.setResult(collect);
        PaginationDto paginationDto = new PaginationDto();
        paginationDto.setPage(paginate.getPage());
        paginationDto.setSize(paginate.getPerPage());
        paginationDto.setTotal(paginate.getTotal());
        paginationDto.setTotalPages(paginate.getTotalPages());
        apiResponseDto.setPagination(paginationDto);
        return apiResponseDto;
    }

    public ApiResponseDto<RD> update(MD mainDto, ME mainEntity, RD responseDto) {
        ME me = mapDtoToEntity(mainDto, mainEntity);
        ME updatedMainEntity = (ME) genericDao.update(me);
        RD rd = mapEntityToDto(updatedMainEntity, responseDto);
        ApiResponseDto<RD> apiResponseDto = new ApiResponseDto<>();
        apiResponseDto.setResult(rd);
        return apiResponseDto;
    }

    public ApiResponseDto<ID> delete(ME mainEntity, ID pk) {
        genericDao.deleteById(mainEntity, pk);
        ApiResponseDto<ID> apiResponseDto = new ApiResponseDto<>();
        apiResponseDto.setResult(pk);
        return apiResponseDto;
    }

    private ME mapDtoToEntity(MD dto, ME entity) {
        return (ME) objectMapper.convertValue(dto, entity.getClass());
    }

    private RD mapEntityToDto(ME entity, RD dto) {
        return (RD) objectMapper.convertValue(entity, dto.getClass());
    }
}
