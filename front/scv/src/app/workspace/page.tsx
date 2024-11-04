"use client";

import { Suspense, useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import FilterDropdown from "@/components/input/FilterDropdown";
import BoardCard from "@/components/card/BoardCard";
import Pagination from "@/components/pagination/Pagination";
import DatasetRadio from "@/components/input/DatasetRadio";
import SearchInput from "@/components/input/SearchInput";
import Loading from "@/components/loading/Loading";
import { useFetchModels } from "@/hooks/models";
import { ModelQueryParams } from "@/types";

function Community() {
  const router = useRouter();
  const searchParams = useSearchParams();
  // 검색 인풋
  // 검색어 상태 추가
  const currentKeyword = searchParams.get("keyword") || "";
  const [searchValue, setSearchValue] = useState(currentKeyword);

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchValue(e.target.value);
  };

  // URL 파라미터 가져오기
  const currentPage = searchParams.get("page")
    ? parseInt(searchParams.get("page")!)
    : 1;
  const currentDataset = searchParams.get("dataset") || "전체";
  const currentOrder = searchParams.get("order") || "추천순";

  // 상태 관리
  const dataset = ["전체", "MNIST", "Fashion", "CIFAR-10", "SVHN", "EMNIST"];
  const [selected, setSelected] = useState(currentDataset);
  const [selectedFilter, setSelectedFilter] = useState(currentOrder);
  const [viewMode, setViewMode] = useState("완료목록"); // "완료목록" 또는 "임시저장"

  // TODO: 필터 옵션 변경
  const filterOptions = ["추천순", "최신순", "오래된순"];

  // Query 파라미터 변환 함수
  const getSortParams = (
    filter: string,
  ): Partial<Pick<ModelQueryParams, "orderBy" | "direction">> => {
    switch (filter) {
      case "최신순":
        return { orderBy: "updatedAt", direction: "desc" };
      case "오래된순":
        return { orderBy: "updatedAt", direction: "asc" };
      case "추천순":
      default:
        return {};
    }
  };

  // TODO: api 내 모델로 변경
  // 모델 데이터 fetch
  const { data, isLoading, error } = useFetchModels({
    page: currentPage - 1,
    size: 12,
    ...getSortParams(selectedFilter),
    dataName: selected !== "전체" ? selected : undefined,
  });

  // URL 업데이트 함수
  const updateURL = (params: { [key: string]: string | number }) => {
    const current = new URLSearchParams(Array.from(searchParams.entries()));

    Object.entries(params).forEach(([key, value]) => {
      if (value) {
        current.set(key, String(value));
      } else {
        current.delete(key);
      }
    });

    router.push(`/workspace?${current.toString()}`);
  };

  // 이벤트 핸들러
  const handleFilterChange = (filter: string) => {
    setSelectedFilter(filter);
    updateURL({ order: filter, page: 1 });
  };

  const handleDatasetChange = (dataset: string) => {
    setSelected(dataset);
    updateURL({ dataset, page: 1 });
  };

  const handleSearchSubmit = (value: string) => {
    updateURL({ keyword: value, page: 1 });
  };

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [currentPage]);

  // data 로드 시 콘솔에 출력
  if (data) {
    console.log(data); // 데이터가 로드되었을 때만 출력
  }

  if (isLoading) return <Loading />;
  if (error) return <div>에러 발생: {error.message}</div>;

  return (
    <div className="flex flex-col gap-10 py-10">
      {/* 필터, 검색 버튼 */}
      <div className="flex items-center justify-between">
        <FilterDropdown
          selectedFilter={selectedFilter}
          onSelectFilter={handleFilterChange}
          filterOptions={filterOptions}
        />
        <DatasetRadio
          options={dataset}
          selected={selected}
          onChange={handleDatasetChange}
        />
        <div className="w-[400px]">
          <SearchInput
            placeholder="모델명을 검색하세요."
            value={currentKeyword}
            onChange={handleSearchChange}
            onSubmit={handleSearchSubmit}
          />
        </div>
      </div>

      {/* TODO: WorkspaceCard로 교체하고 상태관리로 완료목록, 임시저장 나누기 */}
      <div className="flex flex-col items-center justify-center px-10">
        <div className="flex w-full gap-10">
          <button
            onClick={() => setViewMode("완료목록")}
            className="flex rounded-t-10 bg-blue-50 px-40 py-20"
          >
            완료목록
          </button>
          <button
            onClick={() => setViewMode("임시저장")}
            className="flex rounded-t-10 bg-yellow-50 px-40 py-20"
          >
            임시저장
          </button>
        </div>
        {/* boardCard */}
        <div
          className={`grid w-full grid-cols-3 gap-10 rounded-b-10 px-10 py-20 ${
            viewMode === "완료목록" ? "bg-blue-50" : "bg-yellow-50"
          }`}
        >
          {data?.content.length === 0 ? (
            <div>모델이 없습니다.</div> // 데이터가 없을 경우 메시지 출력
          ) : (
            data?.content.map((model) => (
              <BoardCard
                key={model.modelId}
                modelId={model.modelId}
                versionId={`${model.latestNumber}`}
                title={model.modelName}
                version={`v${model.latestNumber}`} // version 값 수정
                dataset={model.dataName}
                // profileImg={model.profileImage || "/profile.png"}
                nickname={model.modelName}
                // accuracy={model.accuracy || "N/A"} // 기본값 설정
                updatedAt={model.updatedAt}
              />
            ))
          )}
        </div>

        {/* 페이지네이션 */}
        <Pagination
          totalItems={data!.content.length || 0}
          currentPage={currentPage}
          pageCount={10}
          itemCountPerPage={12}
        />
      </div>
    </div>
  );
}

export default function Page() {
  return (
    <Suspense fallback={<Loading />}>
      <Community />
    </Suspense>
  );
}